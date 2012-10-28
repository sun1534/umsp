package com.partsoft.umsp.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.partsoft.umsp.DataPacket;

public abstract class AbstractDataPacket implements DataPacket, Externalizable, Cloneable {

	public void writeExternal(ObjectOutput out) throws IOException {
		writeDataOutput(out);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		readDataInput(in);
	}

	protected abstract void writeDataOutput(DataOutput out) throws IOException;

	protected abstract void readDataInput(DataInput in) throws IOException;

	/**
	 * 写入out固定长度的字符串
	 * 
	 * @param out
	 *            {@link DataOutput}
	 * @param s
	 *            字符串
	 * @param fixLength
	 */
	protected static void writeFixedString(DataOutput out, String s, int fixLength) throws IOException {
		if (out == null)
			throw new IOException();
		byte sBytes[] = s == null ? new byte[0] : new byte[s.length()];
		int byLen = sBytes.length;
		for (int i = 0; i < byLen; i++) {
			sBytes[i] = (byte) s.charAt(i);
		}
		int fixZero = 0;
		if (byLen > fixLength) {
			byLen = fixLength;
		} else if (byLen < fixLength) {
			fixZero = fixLength - byLen;
		}
		out.write(sBytes, 0, byLen);

		for (; fixZero > 0; fixZero--) {
			out.writeByte(0);
		}
	}

	/**
	 * 读取固定长度的字符串
	 * 
	 * @param in
	 *            {@link DataInput}
	 * @param fixLength
	 *            固定长度
	 * @return 字符串
	 * @throws IOException
	 */
	protected static String readFixedString(DataInput in, int fixLength) throws IOException {
		char[] bytes = new char[fixLength];
		for (int i = 0; i < fixLength; i++) {
			bytes[i] = (char) in.readByte();
		}

		int valid_length = 0;
		for (int i = 0; (i < bytes.length) && (bytes[i] != 0); i++) {
			valid_length++;
		}

		if (valid_length > 0) {
			return new String(bytes, 0, valid_length);
		}
		return "";
	}

	public AbstractDataPacket clone() {
		try {
			return (AbstractDataPacket) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
	
}
