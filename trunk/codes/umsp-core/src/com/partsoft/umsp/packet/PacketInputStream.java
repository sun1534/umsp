package com.partsoft.umsp.packet;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

public class PacketInputStream extends DataInputStream implements DataInput, ObjectInput {

	public PacketInputStream(InputStream in) {
		super(in);
	}

	public Object readObject() throws ClassNotFoundException, IOException {
		throw new ClassNotFoundException("not support read Object");
	}
	
}
