package com.pavlus.hiqsdr;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by pavlus on 16.02.17.
 */

public class ConfigInterface implements Processor<Config, Config>, Closeable {

protected Collection<Subscriber<? super Config>> subscribers;
protected Collection<Subscription> subscriptions;

@Override
public void subscribe(final Subscriber<? super Config> s) {
	subscribers.add(s);
	s.onSubscribe(new ConfigInterfaceSubscription(s));
}

@Override
public void onSubscribe(final Subscription s) {
	subscriptions.add(s);
}

@Override
public void onNext(final Config config) {
	throw new UnsupportedOperationException("Not implemented yet.");
}

@Override
public void onError(final Throwable t) {
	throw new UnsupportedOperationException("Not implemented yet.");
}

@Override
public void onComplete() {
	throw new UnsupportedOperationException("Not implemented yet.");
}

@Override
public void close() throws IOException {
	throw new UnsupportedOperationException("Not implemented yet.");
}

private class ConfigInterfaceSubscription implements Subscription{
	Subscriber<? super Config> subscriber;
	Publisher<Config> publisher;

	ConfigInterfaceSubscription(Subscriber<? super Config> s) {
		this.subscriber = s;
		this.publisher = ConfigInterface.this;
	}

	@Override
	public void request(final long n) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public void cancel() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
}
