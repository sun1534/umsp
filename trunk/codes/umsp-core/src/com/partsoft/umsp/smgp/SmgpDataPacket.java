package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;

import com.partsoft.umsp.DataPacket;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.AbstractDataPacket;
import com.partsoft.utils.Assert;

public abstract class SmgpDataPacket extends AbstractDataPacket implements DataPacket, Externalizable, Cloneable {

	protected static final int PACKET_HEADER_SIZE = 8;

	public final int requestId;

	public int sequenceId;

	public transient long createTimeMillis = System.currentTimeMillis();

	public boolean enabled = true;

	protected SmgpDataPacket(int command) {
		this.requestId = command;
	}

	public int getRequestId() {
		return requestId;
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		if (out == null)
			new IOException();
		out.writeInt(requestId);
		out.writeInt(sequenceId);
		if (Log.isDebugEnabled()) {
			Log.debug(String.format("%s writeDataOut()%s", getClass().getName(), this.toString()));
		}
	}

	protected void readDataInput(DataInput in) throws IOException {
		if (in == null)
			new IOException();
		int tmp_requestid = in.readInt();
		Assert.isTrue(tmp_requestid == this.requestId);
		sequenceId = in.readInt();
	}

	public int getDataSize() {
		return PACKET_HEADER_SIZE;
	}

	public int getBufferSize() {
		return getDataSize() + Buffer.INT_SIZE;
	}

	public SmgpDataPacket clone() {
		SmgpDataPacket new_obj = (SmgpDataPacket) super.clone();
		new_obj.createTimeMillis = System.currentTimeMillis();
		return new_obj;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() +" [requestId=" + requestId + ", sequenceId=" + sequenceId + ", createTimeMillis="
				+ createTimeMillis + ", enabled=" + enabled + "]";
	}
	
	public int compareTo(Object o) {
		int result = -1;
		if (o instanceof SmgpDataPacket) {
			result = (int) (this.createTimeMillis - ((SmgpDataPacket)o).createTimeMillis);
		}
		return result ;
	}

}
