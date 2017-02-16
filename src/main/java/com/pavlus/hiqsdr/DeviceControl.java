package com.pavlus.hiqsdr;

import org.reactivestreams.Processor;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.reactivex.disposables.Disposable;


/**
 * Represents a control interface over HiQSDR.
 *
 * @author Pavel Remygailo
 */

public interface DeviceControl extends Disposable {


/**
 * @param rxPort UDP port for connection at HiQSDR device.
 * @return {@link SwitchablePooledProcessor} which emits sampled packets from HiQSDR.
 */
SwitchablePooledProcessor<ByteBuffer> getRX(int rxPort) throws IOException;

/**
 * @param txPort -- UDP port for connection at HiQSDR device.
 * @return {@link Processor} which consumes {@link ByteBuffer}s and returns used buffers to subscriber.
 */
Processor<ByteBuffer, ByteBuffer> getTX(int txPort);

/**
 * @param cmdPort UDP port for connection at HiQSDR device.
 * @return {@link ConfigInterface} which observes {@link Config} updates from user
 * and notifies if receives Config from HiQSDR.
 */
ConfigInterface getConfigInterface(int cmdPort);

}
