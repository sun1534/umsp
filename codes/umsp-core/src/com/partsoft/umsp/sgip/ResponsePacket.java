package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.sgip.Constants.BindResults;

@SuppressWarnings("serial")
public class ResponsePacket extends SgipDataPacket {

	public static final int PACKET_SIZE = SgipDataPacket.PACKET_SIZE + Buffer.BYTE_SIZE;

	public byte result = BindResults.ERROR;

	public ResponsePacket(int command) {
		super(command);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeByte(result);
		writeFixedString(out, reserve, RESERVE_SIZE);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		result = in.readByte();
		reserve = readFixedString(in, RESERVE_SIZE);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [result=" + result + ", reserve=" + reserve + "]";
	}
	
	@Override
	public int getDataSize() {
		return PACKET_SIZE;
	}

}
