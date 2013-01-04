package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.sgip.Constants.Commands;
import com.partsoft.umsp.utils.UmspUtils;

public class Deliver extends SgipDataPacket {
	
	private static final long serialVersionUID = 4L;

	public String user_number;

	public String sp_number;

	public byte tp_pid;

	public byte tp_udhi;

	public byte message_coding;

	public int message_length;

	public byte[] message_content;

	public Deliver() {
		super(Commands.DELIVER);
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		writeFixedString(out, user_number, 21);
		writeFixedString(out, sp_number, 21);
		out.writeByte(tp_pid);
		out.writeByte(tp_udhi);
		out.writeByte(message_coding);
		out.writeInt(message_length);
		out.write(message_content);
		writeFixedString(out, reserve, 8);
	}

	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		user_number = readFixedString(in, 21);
		sp_number = readFixedString(in, 21);
		tp_pid = in.readByte();
		tp_udhi = in.readByte();
		message_coding = in.readByte();
		message_length = in.readInt();
		message_content = new byte[message_length];
		in.readFully(message_content);
		reserve = readFixedString(in, 8);
	}

	@Override
	public Deliver clone() {
		return (Deliver) super.clone();
	}

	@Override
	public int getDataSize() {
		return PACKET_SIZE + message_length + 49;
	}

	public String getMessageContent() {
		return tp_udhi == 1 ? UmspUtils.fromGsmBytes(message_content, 6, message_length - 6, message_coding)
				: UmspUtils.fromGsmBytes(message_content, message_coding);
	}

	public void setMessageContent(String msg) {
		setMessageContent(msg, MessageCodes.UCS2);
	}

	public void setMessageContent(String msg, int code) {
		message_coding = (byte) code;
		message_content = UmspUtils.toGsmBytes(msg, code);
		tp_udhi = 0;
		message_length = message_content == null ? 0 : message_content.length;
	}
	
	public int getMessageCascadeCount() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.message_content[4];
		}
		return result & 0xFF;
	}
	
	public int getMessageCascadeRefId() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.message_content[3];
		}
		return result;
	}

	public int getMessageCascadeOrder() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.message_content[5];
		}
		return result & 0xFF;
	}

	@Override
	public String toString() {
		return "SGIPDeliver [command=" + command + ", node_id=" + node_id + ", timestamp=" + timestamp + ", sequence="
				+ sequence + ", user_number=" + user_number + ", sp_number=" + sp_number + ", tp_pid=" + tp_pid
				+ ", tp_udhi=" + tp_udhi + ", message_coding=" + message_coding + ", message_length=" + message_length
				+ ", message_content=" + Arrays.toString(message_content) + ", reserve=" + reserve + "]";
	}

}
