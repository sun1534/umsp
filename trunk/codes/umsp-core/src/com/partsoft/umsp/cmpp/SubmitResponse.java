package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class SubmitResponse extends CmppDataPacket {

	private static final long serialVersionUID = 0x80000004L;
	
	public int nodeId;
	
	public int nodeTime;
	
	public int nodeSeq;

	public int result;

	public SubmitResponse() {
		super(Commands.CMPP_SUBMIT_RESP);
	}
	
	public long getMsgId() {
		return CmppUtils.generateMsgID(this.nodeId, this.nodeTime, this.nodeSeq);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeLong(getMsgId());
		if (protocolVersion == Constants.VERSION3) {
			out.writeInt(this.result);
		} else {
			out.writeByte(this.result);
		}
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		long temp_msgid = in.readLong();
		
		nodeId = CmppUtils.getNodeIdFromMsgID(temp_msgid);
		nodeTime = CmppUtils.getNodeTimeFromMsgID(temp_msgid);
		nodeSeq = CmppUtils.getSequenceIdFromMsgID(temp_msgid);
		
		if (protocolVersion == Constants.VERSION3) {
			this.result = in.readInt();
		} else {
			this.result = in.readUnsignedByte();
		}
	}

	@Override
	public int getDataSize() {
		int result = 0;
		if (protocolVersion == Constants.VERSION3) {
			result = super.getDataSize() + 12;
		} else {
			result = super.getDataSize() + 9;
		}
		return result;
	}

	@Override
	public SubmitResponse clone() {
		return (SubmitResponse) super.clone();
	}

	@Override
	public String toString() {
		return "SubmitResponse [nodeId=" + nodeId + ", nodeTime=" + nodeTime
				+ ", nodeSeq=" + nodeSeq + ", result=" + result + ", sequenceId=" + sequenceId + ", createTimeMillis="
				+ createTimeMillis + ", protocolVersion=" + protocolVersion + "]";
	}
	
}
