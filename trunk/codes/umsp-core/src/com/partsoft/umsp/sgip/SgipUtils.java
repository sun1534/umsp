package com.partsoft.umsp.sgip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.Constants.SMS;
import com.partsoft.umsp.handler.PacketContextHandler;
import com.partsoft.umsp.io.ByteArrayBuffer;
import com.partsoft.umsp.utils.UmspUtils;
import com.partsoft.utils.Assert;
import com.partsoft.utils.RandomUtils;

public abstract class SgipUtils {

	/**
	 * 复制序列号
	 * 
	 * @param dest
	 * @param src
	 */
	public static void copySerialNumber(SgipDataPacket dest, SgipDataPacket src) {
		dest.node_id = src.node_id;
		dest.timestamp = src.timestamp;
		dest.sequence = src.sequence;
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
		List<Submit> packetResults = new LinkedList<Submit>();

		int maxUserCount = well_convert_submit.user_count;
		int nlStart = 0;
		if (messageContext != null && messageContext.length() > 0) {
			int pt = 1;
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
					user_numbers = well_convert_submit.user_number;
				} else {
					nlLength = maxUserCount - nlStart;
					nlLength = nlLength > SMS.MAX_SMS_USER_NUMBERS ? SMS.MAX_SMS_USER_NUMBERS : nlLength;
					user_numbers = new String[nlLength];
					System.arraycopy(well_convert_submit.user_number, nlStart, user_numbers, 0, nlLength);
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
						byte[] mesage_bytes = UmspUtils.toGsmBytes(packetMessage, well_convert_submit.message_coding);
						ByteArrayBuffer byte_buffer = new ByteArrayBuffer(6 + mesage_bytes.length);
						byte_buffer.put((byte) 5);
						byte_buffer.put((byte) 0);
						byte_buffer.put((byte) 3);
						byte_buffer.put(rondPack);
						byte_buffer.put((byte) pt);
						byte_buffer.put((byte) i);
						byte_buffer.put(mesage_bytes);
						Submit submit = well_convert_submit.clone();
						submit.user_count = nlLength;
						submit.user_number = user_numbers;
						submit.tp_udhi = 1;
						submit.message_content = byte_buffer.array();
						submit.message_length = byte_buffer.length();
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
					submit.user_count = nlLength;
					submit.user_number = user_numbers;
					results.add(submit);
				}
				packetResults.addAll(results);
			}
		}
		return new ArrayList<Submit>(packetResults);
	}

	public static void postSubmit(String sgip_smg_host, int port, String sp_number, String enterprise_id, String uid,
			String pwd, Submit[] submits) throws Exception {
		postSubmit(sgip_smg_host, port, sp_number, enterprise_id, uid, pwd, submits, null);
	}

	public static void postSubmit(String sgip_smg_host, int port, String sp_number, String enterprise_id, String uid,
			String pwd, Submit[] submits, PostSubmitListener listener) throws Exception {
		Client client = new Client(Constants.PROTOCOL_NAME, sgip_smg_host, port);
		client.setMaxConnection(3);
		client.setAutoReConnect(true);
		SgipContextSPCHandler sgip_handler = new SgipContextSPCHandler();
		sgip_handler.setAccount(uid);
		sgip_handler.setPassword(pwd);
		sgip_handler.setSpNumber(Integer.parseInt(sp_number));
		sgip_handler.setEnterpriseId(Integer.parseInt(enterprise_id));
		List<Submit> submit_list = Arrays.asList(submits);
		sgip_handler.postSubmit(submit_list, 0, submit_list.size());
		if (listener != null) {
			sgip_handler.addSubmitListener(listener);
		}
		PacketContextHandler pktchr = new PacketContextHandler();
		pktchr.setContextProtocol(Constants.PROTOCOL_NAME);
		pktchr.setHandler(sgip_handler);
		client.setHandler(pktchr);
		client.start();
		client.join();
	}

	public static void main(String[] args) {
		Submit submit = new Submit();
		submit.addUserNumber("13317312768");
		submit.setMessageContent("我测测试");
		try {
			postSubmit("124.126.119.17", 8890, "10690087", "91473", "10690087", "w20243", convertSubmits(submit)
					.toArray(new Submit[0]));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
