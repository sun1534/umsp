package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;

import com.partsoft.umsp.DataPacket;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.packet.AbstractDataPacket;
import com.partsoft.utils.Assert;

public abstract class CmppDataPacket extends AbstractDataPacket implements DataPacket, Externalizable, Cloneable {

	protected static final int PACKET_HEADER_SIZE = 8;

	//消息包识别标识
	public final int commandId;

	//序列号
	public int sequenceId;
	
	//创建时间
	public long createTimeMillis = System.currentTimeMillis();
	
	//协议版本
	public int protocolVersion = Constants.VERSION2;
	
	public boolean enabled = true;

	protected CmppDataPacket(int command) {
		this.commandId = command;
	}
	
	public int getCommandId() {
		return commandId;
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		if (out == null)
			new IOException();
		out.writeInt(commandId);
		out.writeInt(sequenceId);
	}

	protected void readDataInput(DataInput in) throws IOException {
		if (in == null)
			new IOException();
		int cmd = in.readInt();
		Assert.isTrue(cmd == this.commandId, "not validate packet");
		sequenceId = in.readInt();
	}

	public int getDataSize() {
		return PACKET_HEADER_SIZE;
	}

	public int getBufferSize() {
		return getDataSize() + Buffer.INT_SIZE;
	}

	public CmppDataPacket clone() {
		CmppDataPacket new_obj = (CmppDataPacket) super.clone();
		new_obj.createTimeMillis = System.currentTimeMillis();
		return new_obj;
	}

}
