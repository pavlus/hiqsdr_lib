package com.pavlus.hiqsdr;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by pavlus on 03.02.17.
 */

public class Protocol {

static final byte MAGIC_S = 0x53;
static final byte MAGIC_t = 0x74;
static final int CLOCK_RATE = 122_880_000;
static final int UDP_CLK_RATE = CLOCK_RATE / 64;

//-----------------------------Some limits-------------------------------------
// TODO: determine exact frequency limits
public static final int MIN_FREQUENCY = 100_000; // 100 kHz
// actually 64 is prescaler*8, and default prescaler is 8, TBD.
public static final int MAX_FREQUENCY = CLOCK_RATE / 2; // (?)61 MHz, but there is info, that 66 MHz(current)
public static final int MIN_SAMPLE_RATE = 48_000;
public static final int MAX_SAMPLE_RATE = 960_000; // probably 1_920_000 works too, on some fws
public static final int RX_HEADER_SIZE = 2;
public static final int RX_PAYLOAD_SIZE = 1440;
static final int RX_PACKET_SIZE = RX_HEADER_SIZE + RX_PAYLOAD_SIZE; // 1442

//------------------------------Packet sizes-----------------------------------
static final int CMD_PACKET_SIZE = 2;
static final int CFG_PACKET_SIZE = 22;

//------------------------------Commands---------------------------------------
final static ByteBuffer
		START_RECEIVING_CMD = ByteBuffer.allocateDirect(2).put(new byte[]{'r', 'r'}).asReadOnlyBuffer();
final static ByteBuffer STOP_RECEIVING_CMD =
		ByteBuffer.allocateDirect(2).put(new byte[]{'s', 's'}).asReadOnlyBuffer();
final static ByteBuffer REQUEST_CONFIG_CMD =
		ByteBuffer.allocateDirect(2).put(new byte[]{'q', 'q'}).asReadOnlyBuffer();

//--------------------------------TX Modes-------------------------------------
// todo: check names and actual modes
// todo: enum
static final byte TX_MODE_INVALID = 0x0;
static final byte TX_MODE_KEYED_CONTINIOUS_WAVE = 0x1;
static final byte TX_MODE_RECEIVED_PTT = 0x2;
static final byte TX_MODE_EXTENDED_IO = 0x4;
static final byte TX_MODE_HW_CONTNIOUS_WAVE = 0x8;

//------------------------------Sample rates-----------------------------------
static int[] SAMPLE_RATES = {
		960_000,  // decimation 2
		640_000,  // d3
		480_000,  // d4
		384_000,  // d5
		320_000,  // d6
		240_000,  // d8
		192_000,  // d10
		120_000,  // d16
		96_000,   // d20
		60_000,   // d32
		48_000    // d40
};
static byte[] SAMPLE_RATE_CODES = {1, 2, 3, 4, 5, 7, 9, 15, 19, 31, 39}; // decimation-1

public static final int TX_SAMPLE_RATE = 48_000;



//----------------------------Util methods-------------------------------------
static byte sampleRate2Code(final int sampleRate) {
	byte code = -1;
	for (int i = 0; i < SAMPLE_RATES.length; ++i) {
		if (SAMPLE_RATES[i] == sampleRate) {
			code = SAMPLE_RATE_CODES[i];
			break;
		}
	}
	return code;
}

static long frequencyToTunePhase(long frequency) {
	return (long) (((frequency / (double) CLOCK_RATE) * (1L << 32)) + 0.5);
}

static long tunePhaseToFrequency(long phase) {
	return (long) ((phase - 0.5) * CLOCK_RATE / (1L << 32));
}

static int code2SampleRate(byte rxCtrl) {
	return UDP_CLK_RATE / (rxCtrl + 1);
}

public static int[] getSupportedSampleRates() {
	// we don't want't someone to tinker with our data, make a copy
	return Arrays.copyOf(Protocol.SAMPLE_RATES, Protocol.SAMPLE_RATES.length);
}

protected Protocol() {}

}
