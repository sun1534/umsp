package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.sgip.Constants.Commands;
import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public class Submit extends SgipDataPacket {

	public static final int PACKET_SIZE = SgipDataPacket.PACKET_SIZE + 115;

	public String sp_number;

	public String charge_number;

	public int user_count;

	public String[] user_number;

	public String corporation_id;

	public String service_type;

	public byte fee_type;

	public String fee_value;

	public String given_value;

	public int priority;

	public byte agent_flag;

	public byte mo_relate_to_mt_flag = 2;

	public String expire_time;

	public String schedule_time;

	public byte report_flag = 2;

	public byte tp_pid;

	public byte tp_udhi;

	public byte message_coding;

	public byte message_type;

	public int message_length;

	public byte[] message_content;

	public Submit() {
		super(Commands.SUBMIT);
	}

	@Override
	public Submit clone() {
		return (Submit) super.clone();
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		writeFixedString(out, this.sp_number, 21);
		writeFixedString(out, this.charge_number, 21);

		out.writeByte(this.user_count);
		for (int i = 0; i < this.user_count; i++) {
			writeFixedString(out, this.user_number[i], 21);
		}

		writeFixedString(out, this.corporation_id, 5);
		writeFixedString(out, this.service_type, 10);
		out.writeByte(fee_type);
		writeFixedString(out, this.fee_value, 6);
		writeFixedString(out, this.given_value, 6);
		out.writeByte(this.agent_flag);
		out.writeByte(this.mo_relate_to_mt_flag);
		out.writeByte(this.priority);
		writeFixedString(out, this.expire_time, 16);
		writeFixedString(out, this.schedule_time, 16);
		out.writeByte(this.report_flag);
		out.writeByte(this.tp_pid);
		out.writeByte(this.tp_udhi);
		out.writeByte(this.message_coding);
		out.writeByte(this.message_type);
		out.writeInt(this.message_length);
		out.write(this.message_content, 0, this.message_length);
		writeFixedString(out, this.reserve, 8);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		this.sp_number = readFixedString(in, 21);
		this.charge_number = readFixedString(in, 21);

		this.user_count = in.readUnsignedByte();
		this.user_number = new String[this.user_count];
		for (int i = 0; i < this.user_count; i++) {
			this.user_number[i] = readFixedString(in, 21);
		}

		this.corporation_id = readFixedString(in, 5);
		this.service_type = readFixedString(in, 10);
		this.fee_type = in.readByte();
		this.fee_value = readFixedString(in, 6);
		this.given_value = readFixedString(in, 6);
		this.agent_flag = in.readByte();
		this.mo_relate_to_mt_flag = in.readByte();
		this.priority = in.readUnsignedByte();
		this.expire_time = readFixedString(in, 16);
		this.schedule_time = readFixedString(in, 16);
		this.report_flag = in.readByte();
		this.tp_pid = in.readByte();
		this.tp_udhi = in.readByte();
		this.message_coding = in.readByte();
		this.message_type = in.readByte();
		this.message_length = in.readInt();
		this.message_content = new byte[this.message_length];
		in.readFully(this.message_content, 0, this.message_length);
		this.reserve = readFixedString(in, 8);
	}

	@SuppressWarnings("unchecked")
	public void addUserNumber(String userNumber) {
		userNumber = UmspUtils.getStandardPhoneNumberOfCN(userNumber);
		if (!new HashSet<String>(ListUtils.array2List(user_number)).contains(userNumber)) {
			user_count++;
			user_number = (String[]) ListUtils.addToArray(user_number, userNumber, String.class);
		}
	}

	public String getMessageContent() {
		return (tp_udhi == 1) ? UmspUtils.fromGsmBytes(message_content, 6, message_length - 6, message_coding)
				: UmspUtils.fromGsmBytes(message_content, 0, message_length, message_coding);
	}
	
	public void setCascadeMessageContent(String msg, int refid, int count, int index) {
		setCascadeMessageContent(msg, MessageCodes.UCS2, refid, count, index);
	}

	public void setCascadeMessageContent(String msg, int messageCode, int refid, int count, int index) {
		if (msg.length() > SMS.MAX_SMS_CASCADEMSG_CONTENT) {
			throw new IllegalArgumentException(String.format("cascade message length must be less than or equals %d",
					SMS.MAX_SMS_CASCADEMSG_CONTENT));
		}
		byte[] msg_content = UmspUtils.toGsmBytes(msg, messageCode);
		ByteArrayBuffer byte_buffer = new ByteArrayBuffer(msg_content.length + 6);
		byte_buffer.put((byte) 5);
		byte_buffer.put((byte) 0);
		byte_buffer.put((byte) 3);
		byte_buffer.put((byte) refid);
		byte_buffer.put((byte) count);
		byte_buffer.put((byte) index);
		this.tp_udhi = 1;
		this.message_length = byte_buffer.length();
		this.message_content = byte_buffer.array();
	}
	

	public void setMessageContent(String msg) {
		setMessageContent(msg, MessageCodes.UCS2);
	}

	public String getUserNumbers() {
		StringBuffer buffer = new StringBuffer(this.user_count * 21);
		boolean appended = false;
		for (int i = 0; i < this.user_count; i++) {
			if (appended)
				buffer.append(",");
			else
				appended = true;
			buffer.append(user_number[i]);
		}
		return buffer.toString();
	}
	
	public void setUserNumbers(String value) {
		if (StringUtils.hasText(value)) {
			if (value.indexOf(',') >= 0) {
				for (String number : StringUtils.split(value, ",")) {
					if (StringUtils.hasText(number.trim())) {
						addUserNumber(number.trim());
					}
				}
			} else {
				addUserNumber(value.trim());
			}
		}
	}	

	public void setMessageContent(String msg, int code) {
		message_coding = (byte) code;
		message_content = UmspUtils.toGsmBytes(msg, code);
		tp_udhi = 0;
		message_length = message_content == null ? 0 : message_content.length;
	}

	public int getDataSize() {
		return super.getDataSize() + user_count * 21 + message_length + 115;
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
		return "SGIPSubmit [sp_number=" + sp_number + ", charge_number=" + charge_number + ", user_count=" + user_count
				+ ", user_number=" + Arrays.toString(user_number) + ", corporation_id=" + corporation_id
				+ ", service_type=" + service_type + ", fee_type=" + fee_type + ", fee_value=" + fee_value
				+ ", given_value=" + given_value + ", priority=" + priority + ", agent_flag=" + agent_flag
				+ ", mo_relate_to_mt_flag=" + mo_relate_to_mt_flag + ", expire_time=" + expire_time
				+ ", schedule_time=" + schedule_time + ", report_flag=" + report_flag + ", tp_pid=" + tp_pid
				+ ", tp_udhi=" + tp_udhi + ", message_coding=" + message_coding + ", message_type=" + message_type
				+ ", message_length=" + message_length + ", message_content=" + Arrays.toString(message_content)
				+ ", reserve=" + reserve + "]";
	}

}
