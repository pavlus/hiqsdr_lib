package com.pavlus.hiqsdr;

import org.reactivestreams.Processor;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Created by pavlus on 10.06.16.
 */
public class Device implements DeviceControl {

// commands
protected final static ByteBuffer REQUEST_CONFIG_CMD = Protocol.REQUEST_CONFIG_CMD.asReadOnlyBuffer();
//---------------------------------------
protected volatile Config config;
protected String ipAddress;

protected InetAddress remoteAddr;

public Device(InetAddress address) {
	this.remoteAddr = address;
}

@Override
public SwitchablePooledProcessor<ByteBuffer> getRX(final int rxPort) throws IOException {
	return new RXProcessor(remoteAddr, rxPort);
}

@Override
public Processor<ByteBuffer, ByteBuffer> getTX(final int txPort) {
	throw new UnsupportedOperationException();
}

@Override
public ConfigInterface getConfigInterface(final int cmdPort) {
	throw new UnsupportedOperationException();
}

@Override
public void dispose() {
	// todo: dispose processors
	throw new UnsupportedOperationException();
}

@Override
public boolean isDisposed() {
	throw new UnsupportedOperationException();
	//return false;
}

/**
 * todo: port this code to processors
protected boolean initChannels() {
	// configuration channel
	cfgAddr = new InetSocketAddress(remoteAddr, cmdPort);
	try {
		configChannel.socket().setSendBufferSize(Protocol.CFG_PACKET_SIZE * 2);
		configChannel.socket().setReceiveBufferSize(Protocol.CFG_PACKET_SIZE * 2);
		configChannel.configureBlocking(false);
		configChannel.register(configSelector, SelectionKey.OP_READ);
		configChannel.connect(cfgAddr);
	} catch (IOException e) {
		onError("Could not connect to command channel", e);
		configChannel = null;
		return false;
	}

	// RX channel
	rxAddr = new InetSocketAddress(remoteAddr, rxPort);
	try {
		receiverChannel.socket().setSendBufferSize(Protocol.CMD_PACKET_SIZE * 2);
		receiverChannel.socket().setReceiveBufferSize(rxBufSz);
		receiverChannel.configureBlocking(false);
		receiverChannel.connect(rxAddr);
	} catch (IOException e) {
		onError("Could not connect to receiver channel", e);
		return false;
	}
	futureRXChannel.put(receiverChannel);
	return true;
}
*/

}




