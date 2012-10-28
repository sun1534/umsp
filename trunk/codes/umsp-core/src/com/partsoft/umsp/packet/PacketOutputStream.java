package com.partsoft.umsp.packet;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

import com.partsoft.umsp.DataPacket;

public class PacketOutputStream extends DataOutputStream implements DataOutput, ObjectOutput {

	public PacketOutputStream(OutputStream out) {
		super(out);
	}

	public void writeObject(Object obj) throws IOException {
		if (obj instanceof DataPacket) {
			((DataPacket) obj).writeExternal(this);
		} else {
			throw new IOException("only support DataPacket object");
		}
	}
}
