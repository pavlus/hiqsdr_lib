package com.pavlus.hiqsdr;

import org.reactivestreams.Processor;

import java.io.IOException;
/**
 * Created by pavlus on 15.02.17.
 */

/**
 * Processor which can be unreliably temporally switched on/off
 * (set into paused/skipping state and resumed again without resubscription)
 * and reuses objects fed in by Publisher it's subscribed to.
 * If there are no more objects in pool it can request it's publishers for new object,
 * so an object factory can be used for generation.
 *
 * @param <T> type of objects used
 */
public interface SwitchablePooledProcessor<T> extends Processor<T, T> {
/**
 * Ask this Processor to start or continue generating data.
 */
SwitchablePooledProcessor<T> switchOn() throws IOException;

/**
 * Ask this Processor to temporally stop generating data.
 */
SwitchablePooledProcessor<T> switchOff() throws IOException;
}
