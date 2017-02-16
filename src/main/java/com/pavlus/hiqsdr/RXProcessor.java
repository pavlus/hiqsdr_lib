package com.pavlus.hiqsdr;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by pavlus on 15.02.17.
 */

final class RXProcessor implements SwitchablePooledProcessor<ByteBuffer>, Runnable, Closeable {

private final static ByteBuffer START_RECEIVING_CMD = Protocol.START_RECEIVING_CMD.asReadOnlyBuffer();
private final static ByteBuffer STOP_RECEIVING_CMD = Protocol.STOP_RECEIVING_CMD.asReadOnlyBuffer();
private final DatagramChannel channel;
// todo: ReferenceQueue for tracking 'lost' buffers?
private final BlockingQueue<ByteBuffer> pool;
private final Collection<Subscriber<ByteBuffer>> subscribers;
private final Collection<Subscription> subscriptions;
private final Map<ByteBuffer, AtomicInteger> bufferUsageCount;
private volatile boolean done = false;
private Disposable scheduledWorker;

private RXProcessor() throws IOException {
	channel = DatagramChannel.open();
	pool = new LinkedBlockingQueue<>(); // ???: or make ArrayBlockingQueue with large size?
	subscribers = new ArrayDeque<>();
	subscriptions = new ArrayDeque<>();
	bufferUsageCount = new WeakHashMap<>();
}

RXProcessor(final SocketAddress addr) throws IOException {
	this();
	channel.socket().connect(addr);
}


RXProcessor(final InetAddress addr, final int port) throws IOException {
	this();
	channel.socket().connect(addr, port);
}

@Override
public SwitchablePooledProcessor switchOn() {
	try {
		synchronized (START_RECEIVING_CMD) {
			START_RECEIVING_CMD.position(0);
			channel.write(START_RECEIVING_CMD);
		}
	} catch (IOException e) {
		e.printStackTrace();
		onError(e);
	}
	return this;
}

@Override
public SwitchablePooledProcessor switchOff() {
	try {
		synchronized (STOP_RECEIVING_CMD) {
			STOP_RECEIVING_CMD.position(0);
			channel.write(STOP_RECEIVING_CMD);
		}
	} catch (IOException e) {
		e.printStackTrace();
		onError(e);
	}
	return this;
}

@Override
public void subscribe(final Subscriber s) {
	subscribers.add(s);
	s.onSubscribe(new RXSubscription(s));
}

@Override
public void onSubscribe(final Subscription s) {
	subscriptions.add(s);
}

@Override
public void onNext(final ByteBuffer byteBuffer) {
	if (bufferUsageCount.get(byteBuffer).decrementAndGet() <= 0) {
		pool.offer(byteBuffer);
	}
}

@Override
public void onError(final Throwable t) {
	t.printStackTrace();
	for (Subscriber<ByteBuffer> s : subscribers) {
		s.onError(t);
	}
	done = true;
	try {
		close();
	} catch (IOException e) {
		e.printStackTrace();
		throw new RuntimeException(e);
	} finally {
		cleanup();
	}
}

@Override
public void onComplete() {
	for (Subscriber<ByteBuffer> s : subscribers) {
		s.onComplete();
	}
	done = true;
	try {
		close();
	} catch (IOException e) {
		e.printStackTrace();
		onError(e);
	} finally {
		cleanup();
	}
}


@Override
public void close() throws IOException {
	switchOff();
	channel.close();
	if (!done) {
		for (Subscriber<ByteBuffer> s : subscribers) {
			s.onError(new IllegalStateException("close() called before Publisher finished!"));
		}
	}
}

@Override
public void run() {
	try {
		while (!done) {
			ByteBuffer buff = getBuffer();
			int cnt = channel.read(buff);
			if (cnt == Protocol.RX_PACKET_SIZE) {
				publishNext(buff);
			} else {
				// todo: maybe just skip?
				throw new IOException("Received corrupted packet."
				                      + " Size: " + cnt
				                      + ", but" + Protocol.RX_PACKET_SIZE
				                      + " expected.");
			}
		}
		onComplete();
	} catch (IOException e) {
		e.printStackTrace();
		onError(e);
	}
}

private ByteBuffer getBuffer() {
	if (pool.isEmpty()) {
		onEmptyPool();
	}
	ByteBuffer buff = pool.poll();
	return buff;
}

private void publishNext(ByteBuffer data) {
	if (done) return;
	AtomicInteger cnt = bufferUsageCount.get(data);
	for (Subscriber<ByteBuffer> s : subscribers) {
		cnt.incrementAndGet();
		s.onNext(data);
	}
}

protected void onEmptyPool() {
	for (Subscription s : subscriptions) {
		s.request(1);
	}
}

private void start() {
	if (scheduledWorker == null && !done) {
		scheduledWorker = Schedulers.io().scheduleDirect(this);
	}
}

private void unsubscribe(RXSubscription s) {
	subscribers.remove(s.subscriber);
}

private void cleanup() {
	for (Subscription s : subscriptions) {
		s.cancel();
	}
	subscribers.clear();
}

private final class RXSubscription implements Subscription {
	Subscriber subscriber;
	RXProcessor publisher;

	RXSubscription(Subscriber<ByteBuffer> subscriber) {
		this.subscriber = subscriber;
		this.publisher = RXProcessor.this;
	}

	@Override
	public void request(final long n) {
		if (publisher.done) return;
		publisher.start();
	}

	@Override
	public void cancel() {
		publisher.unsubscribe(this);
	}
}
}
