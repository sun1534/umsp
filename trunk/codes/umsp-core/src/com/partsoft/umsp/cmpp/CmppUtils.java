package com.partsoft.umsp.cmpp;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.handler.PacketContextHandler;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.Assert;
import com.partsoft.utils.RandomUtils;

public abstract class CmppUtils {

	public static void main(String[] args) {
		long msgId = generateMsgID(1, 1028183612, 2);
		System.out.println(msgId);
		System.out.println(getNodeTimeFromMsgID(msgId));
		System.out.println(getNodeIdFromMsgID(msgId));
		System.out.println(getSequenceIdFromMsgID(msgId));
	}

	public static int getNodeTimeFromMsgID(long msgid) {
		long temp = (msgid >>> 38) & (long) 0x7FFFFFFFFFL;
		return (int) ((temp & 0x3F) + ((temp >>> 6) & 0x3F) * 100 + ((temp >>> 12) & 0x1F) * 10000
				+ ((temp >>> 17) & 0x1F) * 1000000 + ((temp >>> 22) & 0x0F) * 100000000);
	}

	public static int getNodeIdFromMsgID(long msgid) {
		return (int) ((msgid >>> 16) & 0x3FFFFF);
	}

	public static int getSequenceIdFromMsgID(long msgid) {
		return (int) (msgid & 0xFFFF);
	}

	public static long generateMsgID(int node_id, int create_time, int seq) {
		long result = 0;
		byte time_bytes[] = UmspUtils.toBcdBytes(create_time);
		int second_value = time_bytes.length > 0 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 1, 1) : 0;
		int minute_value = time_bytes.length > 1 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 2, 1) : 0;
		int hours_value = time_bytes.length > 2 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 3, 1) : 0;
		int day_value = time_bytes.length > 3 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 4, 1) : 0;
		int month_value = time_bytes.length > 4 ? UmspUtils.fromBcdBytes(time_bytes, time_bytes.length - 5, 1) : 0;

		result = (second_value & 0x3F) | ((minute_value & 0x3F) << 6) | ((hours_value & 0x1F) << 12)
				| (((day_value & 0x1F)) << 17) | ((month_value & 0x0F) << 22);
		return (result << 38) | ((node_id & 0x3FFFFF) << 16) | (seq & 0xFFFF);
	}

	public static String generateClientToken(String clientid, String sharedSecret, int timestamp) {
		MessageDigest md5_encode = null;
		sharedSecret = sharedSecret == null ? "" : sharedSecret;
		ByteArrayBuffer array_buffer = new ByteArrayBuffer(clientid.length() + sharedSecret.length() + 19);
		try {
			md5_encode = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("can't do md5 encode", e);
		}
		for (int i = 0; i < clientid.length(); i++) {
			array_buffer.put((byte) clientid.charAt(i));
		}

		array_buffer.put(new byte[9]);
		for (int i = 0; i < sharedSecret.length(); i++) {
			array_buffer.put((byte) sharedSecret.charAt(i));
		}
		String timestamp_str = String.format("%010d", timestamp);
		for (int i = 0; i < timestamp_str.length(); i++) {
			array_buffer.put((byte) timestamp_str.charAt(i));
		}
		byte[] md5_bytes = md5_encode.digest(array_buffer.array());
		char[] md5_chars = new char[md5_bytes.length];
		for (int i = 0; i < md5_bytes.length; i++) {
			md5_chars[i] = (char) md5_bytes[i];
		}
		return new String(md5_chars);
	}

	/**
	 * 产生服务端返回密钥
	 * 
	 * @param status
	 * @param authenticator_client
	 * @param sharedSecret
	 *            必须为ascii码字符串
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String generateServerToken(int status, String authenticator_client, String sharedSecret) {
		MessageDigest md5_encode = null;
		sharedSecret = sharedSecret == null ? "" : sharedSecret;
		ByteArrayBuffer array_buffer = new ByteArrayBuffer(Buffer.INT_SIZE + authenticator_client.length()
				+ sharedSecret);
		try {
			md5_encode = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("can't do md5 encode", e);
		}
		array_buffer.putInt(status);
		for (int i = 0; i < authenticator_client.length(); i++) {
			array_buffer.put((byte) authenticator_client.charAt(i));
		}
		for (int i = 0; i < sharedSecret.length(); i++) {
			array_buffer.put((byte) sharedSecret.charAt(i));
		}
		byte[] md5_bytes = md5_encode.digest(array_buffer.array());
		char[] md5_chars = new char[md5_bytes.length];
		for (int i = 0; i < md5_bytes.length; i++) {
			md5_chars[i] = (char) md5_bytes[i];
		}
		return new String(md5_chars);
	}

	/**
	 * 短连接发送（不重试，出错直接返回）
	 * 
	 * @param smgp_smg_host
	 * @param port
	 * @param sp_number
	 * @param enterprise_id
	 * @param uid
	 * @param pwd
	 * @param submits
	 * @throws Exception
	 */
	public static void postSubmit(String smgp_smg_host, int port, String sp_number, String enterprise_id, String uid,
			String pwd, Submit[] submits) throws Exception {
		postSubmit(smgp_smg_host, port, sp_number, enterprise_id, uid, pwd, submits, null);
	}

	/**
	 * 短连接发送（不重试，出错直接返回）
	 * 
	 * @param smgp_smg_host
	 * @param port
	 * @param sp_number
	 * @param enterprise_id
	 * @param uid
	 * @param pwd
	 * @param submits
	 * @param listener
	 * @throws Exception
	 */
	public static void postSubmit(String smgp_smg_host, int port, String sp_number, String enterprise_id, String uid,
			String pwd, Submit[] submits, PostSubmitListener listener) throws Exception {
		Client client = new Client(Constants.PROTOCOL_NAME, smgp_smg_host, port);
		client.setAutoReConnect(false);
		CmppContextSPCHandler smgp_handler = new CmppContextSPCHandler();
		smgp_handler.setAccount(uid);
		smgp_handler.setPassword(pwd);
		smgp_handler.setSpNumber(Integer.parseInt(sp_number));
		smgp_handler.setEnterpriseId(Integer.parseInt(enterprise_id));
		List<Submit> submits_list = new LinkedList<Submit>();
		for (Submit sb : submits) {
			submits_list.addAll(convertSubmits(sb));
		}
		smgp_handler.postSubmit(submits_list, 0, submits_list.size());
		if (listener != null) {
			smgp_handler.addSubmitListener(listener);
		}

		PacketContextHandler pktchr = new PacketContextHandler();
		pktchr.setContextProtocol(Constants.PROTOCOL_NAME);
		pktchr.setHandler(smgp_handler);
		client.setHandler(pktchr);
		client.start();
		client.join();
	}

	/**
	 * 把submit转换为Submit列表<br>
	 * <ul>
	 * <li>如果目标接收电话太多则自动分条目</li>
	 * <li>如果内容过长则自动俺长短信模式分条目</li>
	 * </ul>
	 * 
	 * @param well_convert_submit
	 * @return
	 */
	public static List<Submit> convertSubmits(Submit well_convert_submit) {

		String messageContext = well_convert_submit.getMessageContent();

		int pt = messageContext.length() / SMS.MAX_SMS_ONEMSG_CONTENT;
		pt = messageContext.length() % SMS.MAX_SMS_ONEMSG_CONTENT > 0 ? pt + 1 : pt;
		Assert.isTrue(pt <= SMS.MAX_SMS_CASCADES, String.format("短信内容最大长度为%s个字", SMS.MAX_SMS_TOTAL_CONTENT));

		List<Submit> packetResults = new LinkedList<Submit>();

		int maxUserCount = well_convert_submit.destUserCount;
		int nlStart = 0;
		if (messageContext != null && messageContext.length() > 0) {
			pt = 1;
			if (messageContext.length() > SMS.MAX_SMS_ONEMSG_CONTENT) {
				pt = messageContext.length() / SMS.MAX_SMS_CASCADEMSG_CONTENT;
				pt = messageContext.length() % SMS.MAX_SMS_CASCADEMSG_CONTENT > 0 ? pt + 1 : pt;
			}
			Assert.isTrue(pt <= SMS.MAX_SMS_CASCADES);

			while (nlStart < maxUserCount) {
				List<Submit> results = new LinkedList<Submit>();

				String[] user_numbers = null;
				int nlLength = maxUserCount;
				if (maxUserCount <= SMS.MAX_SMS_USER_NUMBERS) {
					user_numbers = well_convert_submit.destTerminalIds;
				} else {
					nlLength = maxUserCount - nlStart;
					nlLength = nlLength > SMS.MAX_SMS_USER_NUMBERS ? SMS.MAX_SMS_USER_NUMBERS : nlLength;
					user_numbers = new String[nlLength];
					System.arraycopy(well_convert_submit.destTerminalIds, nlStart, user_numbers, 0, nlLength);
				}
				nlStart += nlLength;
				if (pt > 1) {
					int i = 0;
					int blStart = 0;
					int blEnd = SMS.MAX_SMS_CASCADEMSG_CONTENT;
					byte rondPack = (byte) (((byte) (RandomUtils.randomInteger() % 255)) & 0xFF);
					while (blStart < messageContext.length()) {
						i = i + 1;
						String packetMessage = messageContext.substring(blStart, blEnd);
						byte[] mesage_bytes = UmspUtils.toGsmBytes(packetMessage, well_convert_submit.msgFormat);
						ByteArrayBuffer byte_buffer = new ByteArrayBuffer(6 + mesage_bytes.length);
						byte_buffer.put((byte) 5);
						byte_buffer.put((byte) 0);
						byte_buffer.put((byte) 3);
						byte_buffer.put(rondPack);
						byte_buffer.put((byte) pt);
						byte_buffer.put((byte) i);
						byte_buffer.put(mesage_bytes);
						Submit submit = well_convert_submit.clone();
						submit.destUserCount = nlLength;
						submit.destTerminalIds = user_numbers;
						submit.tp_udhi = 1;
						submit.pkNumber = (byte) i;
						submit.pkTotal = pt;
						submit.msgContent = byte_buffer.array();
						submit.msgLength = byte_buffer.length();
						results.add(submit);
						blStart += packetMessage.length();
						blEnd += packetMessage.length();
						if (blEnd > messageContext.length()) {
							blEnd = messageContext.length();
						}
					}
				}
				if (results.size() <= 0) {
					Submit submit = well_convert_submit.clone();
					submit.destUserCount = nlLength;
					submit.destTerminalIds = user_numbers;
					results.add(submit);
				}
				packetResults.addAll(results);
			}
		}
		return new ArrayList<Submit>(packetResults);
	}

}
