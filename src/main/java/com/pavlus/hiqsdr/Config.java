package com.pavlus.hiqsdr;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.pavlus.hiqsdr.Protocol.*;

/**
 * Represents a model of HiQSDR configuration.
 *
 * @author Pavel Remygailo
 */
public class Config {

ByteBuffer cmdPacket = ByteBuffer.allocate(CFG_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN); // received packet

ByteBuffer ctrlCmdBuf = ByteBuffer.allocate(CFG_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN); // sent packet
byte txPowerLevel;
byte txControl;
byte rxControl;
byte firmwareVersion;
byte preselector;
byte attenuator;
byte antenna;
long rxTunePhase;
long txTunePhase;
int sampleRate;
long txTuneFrequency;
long rxTuneFrequency;


boolean tieTX2RXFreq = true;
private AtomicBoolean needToFillPacket = new AtomicBoolean(true);

/**
 * Creates Config representing corresponding to specified packet.
 *
 * @param packet
 * @throws IllegalArgumentException if packet can't be parsed.
 */
public Config(ByteBuffer packet) {
	fillFromPacket(packet);
}

/**
 * Creates Config with latest supported firmware capability.
 */
public Config() {
	this((byte) 0x02);
}

/**
 * Creates Config with specified firmware capability.
 *
 * @param fwVersion
 * @throws IllegalArgumentException
 */
public Config(byte fwVersion) {
	checkFwVersionAndSet(fwVersion);
}

/**
 * @return TX power level in range 0..255.
 */
public int getTxPowerLevel() {
	return txPowerLevel & 0xff;
}

/**
 * Sets TX power level. Specified value must be in range 0..255.
 *
 * @param powerLevel
 * @throws IllegalArgumentException
 */
public void setTxPowerLevel(int powerLevel) {
	checkTxPowerLevelAndSet(powerLevel);
	needToFillPacket.set(true);
}

protected void checkTxPowerLevelAndSet(final int powerLevel) {
	if (powerLevel > 255 || powerLevel < 0) {
		throw new IllegalArgumentException("TxPowerLevel must be in range 0-255.");
	}
	txPowerLevel = (byte) (powerLevel & 0xff);
}

public byte getFirmwareVersion() {
	return firmwareVersion;
}

public void setFirmwareVersion(byte fwv) {
	checkFwVersionAndSet(fwv);
	needToFillPacket.set(true);
}


protected void checkFwVersionAndSet(final byte fwv) {
	if (fwv > 2 || fwv < 0) {
		throw new IllegalArgumentException("Supported firmware versions: 0, 1, 2, but \"" + fwv + "\" specified.");
	}
	firmwareVersion = fwv;
}

public byte getAntenna() {
	return antenna;
}

public void setAntenna(byte ant) {
	checkAntennaAndSet(ant);
	needToFillPacket.set(true);
}

protected void checkAntennaAndSet(final byte ant) {
	if (firmwareVersion == 0) {
		throw new IllegalStateException("Antenna selection is not supported by HiQSDR fw v1.0");
	}
	antenna = ant;
}

public synchronized void fillFromChannel(ReadableByteChannel channel) throws IllegalArgumentException, IOException {
	cmdPacket.clear();
	channel.read(cmdPacket);
	fillFromPacket(cmdPacket);
}

public synchronized void fillFromPacket(ByteBuffer packet) throws IllegalArgumentException {
	packet.order(ByteOrder.LITTLE_ENDIAN);

	// check header magic
	byte S = packet.get();
	byte t = packet.get();
	if (S != MAGIC_S || t != MAGIC_t) {
		throw new IllegalArgumentException("Malformed packet");
	}

	rxTunePhase = packet.getInt();
	rxTuneFrequency = tunePhaseToFrequency(rxTunePhase);

	txTunePhase = packet.getInt();
	txTuneFrequency = tunePhaseToFrequency(txTunePhase);
	if (rxTuneFrequency == txTuneFrequency) {
		tieTX2RXFreq = true;
	}

	txPowerLevel = packet.get();

	txControl = packet.get();

	rxControl = packet.get();

	sampleRate = code2SampleRate(rxControl);

	firmwareVersion = packet.get();
	if (firmwareVersion < 1) {
		preselector = 0;
		attenuator = 0;
		antenna = 0;
	} else {
		preselector = packet.get();
		attenuator = packet.get();
		antenna = packet.get();
	}
}

public byte getTxMode() {
	return txControl;
}

public void setTxMode(byte mode) {
	switch (mode) {
		case TX_MODE_EXTENDED_IO:
		case TX_MODE_HW_CONTNIOUS_WAVE:
			if (firmwareVersion == 0) {
				throw new IllegalStateException("Specified mode is not supported by HiQSDR firmware v1.0");
			}
		case TX_MODE_KEYED_CONTINIOUS_WAVE:
		case TX_MODE_RECEIVED_PTT:
			txControl = mode;
			break;
		default:
			throw new IllegalArgumentException("Unknown TX mode " + mode + '.');
	}

	needToFillPacket.set(true);
}

public synchronized void setRxFrequency(long frequency) {
	rxTuneFrequency = frequency;
	rxTunePhase = frequencyToTunePhase(frequency);

	needToFillPacket.set(true);
}

public synchronized long getRxFrequency() {
	return rxTuneFrequency;
}

public synchronized void setTxFrequency(long frequency) {
	txTuneFrequency = frequency;
	txTunePhase = frequencyToTunePhase(frequency);

	needToFillPacket.set(true);
}


public synchronized long getTxFrequency() {
	return txTuneFrequency;
}

public void setTiedTxToRxFreq(boolean tie) {
	tieTX2RXFreq = tie;
	needToFillPacket.set(true);
}

public boolean isTiedTxToRxFreq() {
	return tieTX2RXFreq;
}

public void setSampleRate(int sampleRate) throws IllegalArgumentException {
	// lazy
	if (this.sampleRate == sampleRate) {
		return;
	}

	if (sampleRate <= 0) {
		throw new IllegalArgumentException("Sample rate must be positive number and one of supported values.");
	}

	final byte code = sampleRate2Code(sampleRate);
	if (code < 0) {
		throw new IllegalArgumentException("Specified sample rate (" + sampleRate + " is not supported");
	}

	this.sampleRate = sampleRate;
	rxControl = code;
	needToFillPacket.set(true);

}


/**
 * Writes configuration in binary form to the specified buffer.
 *
 * @param buffer
 * @throws IOException
 */
public synchronized void writeOut(ByteBuffer buffer) {
	fillCtrlPacket();
	buffer.put(ctrlCmdBuf);
	ctrlCmdBuf.flip(); // for future usage
}

/**
 * Writes configuration in binary form to the specified inputChannel.
 *
 * @param channel
 * @throws IOException
 */
public synchronized void writeOut(WritableByteChannel channel) throws IOException {
	fillCtrlPacket();
	channel.write(ctrlCmdBuf);
	ctrlCmdBuf.flip(); // for future usage
}

/**
 * Serializes representation to HiQSDR config protocol.
 */
protected void fillCtrlPacket() {

	if (ctrlCmdBuf.remaining() != CFG_PACKET_SIZE || needToFillPacket.getAndSet(false)) {
		ctrlCmdBuf.clear();

		ctrlCmdBuf
				.put(MAGIC_S) // 'S'
				.put(MAGIC_t) // 't'

				.putInt((int) (rxTunePhase))

				.putInt((int) (tieTX2RXFreq ? rxTunePhase : txTunePhase))

				.put(txPowerLevel)

				.put(txControl)

				.put(rxControl)

				.put(firmwareVersion);

		if (firmwareVersion < 1) {
			ctrlCmdBuf
					.put((byte) 0)
					.put((byte) 0)
					.put((byte) 0);
		} else {
			ctrlCmdBuf
					.put(preselector)

					.put(attenuator)

					.put(antenna);
		}

		// reserved
		ctrlCmdBuf
				.put((byte) 0)
				.put((byte) 0)
				.put((byte) 0)
				.put((byte) 0)
				.put((byte) 0);

		ctrlCmdBuf.flip();
	}
}


/**
 * Checks if received packet equals to sent packet (is device config consistent with representation)
 *
 * @return true if received packet matches sent packet, false otherwise
 */
public boolean isConsistent() {
	return cmdPacket.equals(ctrlCmdBuf);
}

@Override
public String toString() {
	StringBuilder sb = new StringBuilder("HiQSDRconfig: [");
	sb.append("rxFreq: ").append(rxTuneFrequency);
	sb.append(", txFreq: ").append(txTuneFrequency);
	sb.append(", tieTxToRx: ").append(tieTX2RXFreq);
	sb.append(", txPowerLevel: ").append(txPowerLevel);
	sb.append(", txMode: ").append(Integer.toBinaryString(txControl));
	sb.append(", rxMode: ").append(Integer.toBinaryString(rxControl));
	sb.append(", firmwareVersion: ").append(firmwareVersion);
	if (firmwareVersion >= 1) {
		sb.append(", preselector: ").append(preselector);
		sb.append(", attenuator: ").append(attenuator);
		sb.append(", antenna: ").append(antenna);
	}
	sb.append(']');
	return sb.toString();
}

}
