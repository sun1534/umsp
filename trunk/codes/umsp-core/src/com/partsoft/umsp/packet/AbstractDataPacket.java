package com.partsoft.umsp.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import com.partsoft.umsp.DataPacket;

@SuppressWarnings("serial")
public abstract class AbstractDataPacket implements DataPacket, Serializable, Cloneable {
	
	public String srev1;
	
	public String srev2;
	
	public String srev3;
	
	public String srev4;
	
	public String srev5;
	
	public int irev1;
	
	public int irev2;
	
	public int irev3;

	protected String dataPacketId = UUID.randomUUID().toString();

	public String getDataPacketId() {
		return dataPacketId;
	}

	public void setDataPacketId(String dataPacketId) {
		this.dataPacketId = dataPacketId;
	}

	public void writeExternal(DataOutput out) throws IOException {
		writeDataOutput(out);
	}

	public void readExternal(DataInput in) throws IOException {
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
			AbstractDataPacket cloned = (AbstractDataPacket) super.clone();
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}

}
