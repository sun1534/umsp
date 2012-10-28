package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.sgip.Constants.Commands;

public class UnBindResponse extends ResponsePacket {
	
	public UnBindResponse() {
		super(Commands.UNBIND_RESPONSE);
	}
	
	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		if (out == null)
			new IOException();
		out.writeInt(command);
		out.writeInt(node_id);
		out.writeInt(timestamp);
		out.writeInt(sequence);
	}
	
	@Override
	protected void readDataInput(DataInput in) throws IOException {
		if (in == null)
			new IOException();
		node_id = in.readInt();
		timestamp = in.readInt();
		sequence = in.readInt();
	}
	
	@Override
	public int getDataSize() {
		return PACKET_SIZE - RESERVE_SIZE;
	}

	@Override
	public UnBindResponse clone() {
		return (UnBindResponse) super.clone();
	}

}
