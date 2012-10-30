package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import com.partsoft.umsp.Constants.MessageCodes;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.umsp.smgp.Constants.TlvTags;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.Assert;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public class Submit extends SmgpTlvDataPacket {

	public byte MsgType;// 1 integer 0＝MO 消息（终端发给SP）；
	// 6＝MT 消息（SP 发给终端，包括WEB 上发送的点对点短消息）；
	// 7＝点对点短消息；

	public byte NeedReport;// 1 integer 是否要求返回状态报告（0=不要求，=要求）

	public int Priority;// 1 Integer 发送优先级（从0到9）

	public String ServiceID;// 10 Octet String 业务类型

	public String FeeType;// 2 Octet String 收费类型（参见收费类型代码表）

	public String FeeCode;// 6 Octet String 资费代码（单位为分）

	public String FixedFee;// 6

	public byte MsgFormat;// 1 Octet String 短消息格式（参见短消息格式代码表）

	public String ValidTime;// 17 Octet String 有效时间，格式遵循SMPP3.3协议

	public String AtTime;// 17 Octet String 定时发送时间，格式遵循SMPP3.3协议

	public String SrcTermID;// 21 Octet String 短消息发送用户号码

	public String ChargeTermID;// 21 Octet String 计费用户号码

	public int DestTermIDCount;// 1 Integer 短消息接收号码总数（≤100）

	public String[] DestTermID;// 21* DestTerm Count Octet String
								// 短消息接收号码（连续存储DestTermIDCount个号码）
	public int MsgLength;// 1 Integer 短消息长度

	public byte[] MsgContent;// ≤252 Octet String 短消息内容

	public String Reserve;// 8 Octet String 保留

	protected byte tp_udhi;

	public Submit() {
		super(RequestIDs.submit);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeByte(MsgType);
		out.writeByte(NeedReport);
		out.writeByte(Priority);
		writeFixedString(out, ServiceID, 10);
		writeFixedString(out, FeeType, 2);
		writeFixedString(out, FeeCode, 6);
		writeFixedString(out, FixedFee, 6);
		out.writeByte(MsgFormat);
		writeFixedString(out, ValidTime, 17);
		writeFixedString(out, AtTime, 17);
		writeFixedString(out, SrcTermID, 21);
		writeFixedString(out, ChargeTermID, 21);
		out.writeByte(DestTermIDCount);
		for (int i = 0; i < DestTermIDCount; i++) {
			writeFixedString(out, DestTermID[i], 21);
		}
		out.writeByte(MsgLength);
		out.write(MsgContent, 0, MsgLength);
		writeFixedString(out, Reserve, 8);
		writeTlvDatas(out);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		MsgType = in.readByte();
		NeedReport = in.readByte();
		Priority = in.readByte();
		ServiceID = readFixedString(in, 10);
		FeeType = readFixedString(in, 2);
		FeeCode = readFixedString(in, 6);
		FixedFee = readFixedString(in, 6);
		MsgFormat = in.readByte();
		ValidTime = readFixedString(in, 17);
		AtTime = readFixedString(in, 17);
		SrcTermID = readFixedString(in, 21);
		ChargeTermID = readFixedString(in, 21);
		DestTermIDCount = in.readUnsignedByte();
		Assert.isTrue(DestTermIDCount <= 100);
		DestTermID = new String[DestTermIDCount];
		for (int i = 0; i < DestTermIDCount; i++) {
			DestTermID[i] = readFixedString(in, 21);
		}
		MsgLength = in.readUnsignedByte();
		Assert.isTrue(MsgLength <= 252);
		MsgContent = new byte[MsgLength];
		in.readFully(MsgContent, 0, MsgLength);
		Reserve = readFixedString(in, 8);
		readTlvDatas(in);
		tp_udhi = this.hasDynamicProperty(TlvTags.TP_udhi) ? getDynamicProperty(TlvTags.TP_udhi)[0] : 0;
	}

	@Override
	public int getDataSize() {
		return super.getDataSize() + 114 + DestTermIDCount * 21 + MsgLength;
	}

	public void setMessageContent(String msg, int code) {
		MsgFormat = (byte) code;
		MsgContent = UmspUtils.toGsmBytes(msg, code);
		tp_udhi = 0;
		MsgLength = MsgContent == null ? 0 : MsgContent.length;
	}

	public void setMessageContent(String msg) {
		setMessageContent(msg, MessageCodes.UCS2);
	}

	@SuppressWarnings("unchecked")
	public void addUserNumber(String userNumber) {
		userNumber = UmspUtils.getStandardPhoneNumberOfCN(userNumber);
		if (!new HashSet<String>(ListUtils.array2List(DestTermID)).contains(userNumber)) {
			DestTermIDCount++;
			DestTermID = (String[]) ListUtils.addToArray(DestTermID, userNumber, String.class);
		}
	}
	
	public void setUserNumbers(String value) {
		if (StringUtils.hasText(value)) {
			if (value.indexOf(',') >= 0) {
				for (String number : StringUtils.split(value, ",")) {
					addUserNumber(number.trim());
				}
			} else {
				addUserNumber(value.trim());
			}
		}
	}

	public String getUserNumbers() {
		StringBuffer buffer = new StringBuffer(this.DestTermIDCount * 21);
		boolean appended = false;
		for (int i = 0; i < this.DestTermIDCount; i++) {
			if (appended)
				buffer.append(",");
			else
				appended = true;
			buffer.append(DestTermID[i]);
		}
		return buffer.toString();
	}

	public int getMessageCascadeCount() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.MsgContent[4];
		}
		return result & 0xFF;
	}

	public int getMessageCascadeRefId() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.MsgContent[3];
		}
		return result;
	}

	public int getMessageCascadeOrder() {
		byte result = 0;
		if (this.tp_udhi == 1) {
			result = this.MsgContent[5];
		}
		return result & 0xFF;
	}

	public String getMessageContent() {
		return (tp_udhi == 1) ? UmspUtils.fromGsmBytes(MsgContent, 6, MsgLength - 6, MsgFormat) : UmspUtils
				.fromGsmBytes(MsgContent, 0, MsgLength, MsgFormat);
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
		this.MsgLength = byte_buffer.length();
		this.MsgContent = byte_buffer.array();
	}
	
	@Override
	public Submit clone() {
		return (Submit) super.clone();
	}

	@Override
	public String toString() {
		return "Submit [MsgType=" + MsgType + ", NeedReport=" + NeedReport + ", Priority=" + Priority + ", ServiceID="
				+ ServiceID + ", FeeType=" + FeeType + ", FeeCode=" + FeeCode + ", FixedFee=" + FixedFee
				+ ", MsgFormat=" + MsgFormat + ", ValidTime=" + ValidTime + ", AtTime=" + AtTime + ", SrcTermID="
				+ SrcTermID + ", ChargeTermID=" + ChargeTermID + ", DestTermIDCount=" + DestTermIDCount
				+ ", DestTermID=" + Arrays.toString(DestTermID) + ", MsgLength=" + MsgLength + ", MsgContent="
				+ Arrays.toString(MsgContent) + ", Reserve=" + Reserve + "]";
	}
}
