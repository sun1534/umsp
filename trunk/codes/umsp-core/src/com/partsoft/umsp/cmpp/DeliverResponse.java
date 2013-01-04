package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class DeliverResponse extends CmppDataPacket {
	
	private static final long serialVersionUID = 0x80000005L;

	public int nodeId;
	
	public int nodeTime;
	
	public int nodeSeq;

	public int result;

	public DeliverResponse() {
		super(Commands.CMPP_DELIVER_RESP);
	}

	public long getMsgId() {
		return CmppUtils.generateMsgID(this.nodeId, this.nodeTime, this.nodeSeq);
	}
	
	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeLong(this.getMsgId());
		out.writeInt(this.result);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		long tmp_msgid = in.readLong();
		this.nodeId = CmppUtils.getNodeIdFromMsgID(tmp_msgid);
		this.nodeTime = CmppUtils.getNodeTimeFromMsgID(tmp_msgid);
		this.nodeSeq  = CmppUtils.getSequenceIdFromMsgID(tmp_msgid);
		this.result = in.readInt();
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + 12;
	}

	@Override
	public DeliverResponse clone() {
		return (DeliverResponse) super.clone();
	}

	@Override
	public String toString() {
		return "DeliverResponse [nodeId=" + nodeId + ", nodeTime=" + nodeTime + ", result=" + result + ", commandId="
				+ commandId + ", sequenceId=" + sequenceId + ", createTimeMillis=" + createTimeMillis
				+ ", protocolVersion=" + protocolVersion + ", getMsgId()=" + getMsgId() + "]";
	}

}
