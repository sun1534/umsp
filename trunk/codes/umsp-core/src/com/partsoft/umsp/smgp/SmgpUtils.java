package com.partsoft.umsp.smgp;

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
import com.partsoft.umsp.smgp.Constants.TlvTags;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.Assert;
import com.partsoft.utils.HexUtils;
import com.partsoft.utils.RandomUtils;

public abstract class SmgpUtils {
	
	public static void main(String[] args) {
		byte bytes[] = HexUtils.bytesFromHex("73106210301608267738");
		System.out.println("" + UmspUtils.fromBcdBytes(bytes, 3, 4));
		
	}

	public static byte[] generateMsgID(int node_id, int create_time, int seq) {
		byte[] node_id_bytes = UmspUtils.toBcdBytes(node_id);
		byte[] create_time_bytes = UmspUtils.toBcdBytes(create_time);
		byte[] seq_bytes = UmspUtils.toBcdBytes(seq);

		if (node_id_bytes.length < 3) {
			ByteArrayBuffer node_id_bytes_buffer = new ByteArrayBuffer(3);
			node_id_bytes_buffer.put(new byte[3 - node_id_bytes.length]);
			node_id_bytes_buffer.put(node_id_bytes);
			node_id_bytes = node_id_bytes_buffer.array();
		} else if (node_id_bytes.length > 3) {
			ByteArrayBuffer node_id_bytes_buffer = new ByteArrayBuffer(node_id_bytes, node_id_bytes.length - 3, 3);
			node_id_bytes = node_id_bytes_buffer.asArray();
		}

		if (create_time_bytes.length < 4) {
			ByteArrayBuffer create_time_bytes_buffer = new ByteArrayBuffer(3);
			create_time_bytes_buffer.put(new byte[4 - create_time_bytes.length]);
			create_time_bytes_buffer.put(create_time_bytes);
			create_time_bytes = create_time_bytes_buffer.array();
		} else if (create_time_bytes.length > 4) {
			ByteArrayBuffer create_time_bytes_buffer = new ByteArrayBuffer(create_time_bytes,
					create_time_bytes.length - 4, 4);
			create_time_bytes = create_time_bytes_buffer.asArray();
		}

		if (seq_bytes.length < 3) {
			ByteArrayBuffer seq_bytes_buffer = new ByteArrayBuffer(3);
			seq_bytes_buffer.put(new byte[3 - seq_bytes.length]);
			seq_bytes_buffer.put(seq_bytes);
			seq_bytes = seq_bytes_buffer.array();
		} else if (seq_bytes.length > 3) {
			ByteArrayBuffer seq_bytes_buffer = new ByteArrayBuffer(seq_bytes, seq_bytes.length - 3, 3);
			seq_bytes = seq_bytes_buffer.asArray();
		}

		ByteArrayBuffer result_buffer = new ByteArrayBuffer(10);
		result_buffer.put(node_id_bytes);
		result_buffer.put(create_time_bytes);
		result_buffer.put(seq_bytes);
		return result_buffer.array();
	}

	public static byte[] generateClientToken(String clientid, String sharedSecret, int timestamp) {
		MessageDigest md5_encode = null;
		sharedSecret = sharedSecret == null ? "" : sharedSecret;
		ByteArrayBuffer array_buffer = new ByteArrayBuffer(clientid.length() + sharedSecret.length() + 17);
		try {
			md5_encode = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("can't do md5 encode", e);
		}
		array_buffer.put(clientid.getBytes());
		array_buffer.put(new byte[7]);
		array_buffer.put(sharedSecret.getBytes());
		array_buffer.put(String.format("%010d", timestamp).getBytes());

		return md5_encode.digest(array_buffer.array());
	}

	public static byte[] generateServerToken(int status, byte[] authenticator_client, String sharedSecret) {
		MessageDigest md5_encode = null;
		sharedSecret = sharedSecret == null ? "" : sharedSecret;
		ByteArrayBuffer array_buffer = new ByteArrayBuffer(Buffer.INT_SIZE + authenticator_client.length + sharedSecret);
		try {
			md5_encode = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("can't do md5 encode", e);
		}
		array_buffer.putInt(status);
		array_buffer.put(authenticator_client);
		array_buffer.put(sharedSecret.getBytes());
		return md5_encode.digest(array_buffer.array());
	}

	/**
	 * 短连接发送（不重试，出错直接返回）
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
		SmgpContextSPCHandler smgp_handler = new SmgpContextSPCHandler();
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

		int maxUserCount = well_convert_submit.DestTermIDCount;
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
					user_numbers = well_convert_submit.DestTermID;
				} else {
					nlLength = maxUserCount - nlStart;
					nlLength = nlLength > SMS.MAX_SMS_USER_NUMBERS ? SMS.MAX_SMS_USER_NUMBERS : nlLength;
					user_numbers = new String[nlLength];
					System.arraycopy(well_convert_submit.DestTermID, nlStart, user_numbers, 0, nlLength);
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
						byte[] mesage_bytes = UmspUtils.toGsmBytes(packetMessage, well_convert_submit.MsgFormat);
						ByteArrayBuffer byte_buffer = new ByteArrayBuffer(6 + mesage_bytes.length);
						byte_buffer.put((byte) 5);
						byte_buffer.put((byte) 0);
						byte_buffer.put((byte) 3);
						byte_buffer.put(rondPack);
						byte_buffer.put((byte) pt);
						byte_buffer.put((byte) i);
						byte_buffer.put(mesage_bytes);
						Submit submit = well_convert_submit.clone();
						submit.DestTermIDCount = nlLength;
						submit.DestTermID = user_numbers;
						submit.tp_udhi = 1;
						submit.setDynamicProperty(TlvTags.TP_udhi, new byte[] { 1 });
						submit.setDynamicProperty(TlvTags.PkTotal, new byte[] { (byte) pt });
						submit.setDynamicProperty(TlvTags.PkNumber, new byte[] { (byte) i });
						submit.MsgContent = byte_buffer.array();
						submit.MsgLength = byte_buffer.length();
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
					submit.DestTermIDCount = nlLength;
					submit.DestTermID = user_numbers;
					results.add(submit);
				}
				packetResults.addAll(results);
			}
		}
		return new ArrayList<Submit>(packetResults);
	}

}
