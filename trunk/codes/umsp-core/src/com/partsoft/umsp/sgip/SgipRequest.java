package com.partsoft.umsp.sgip;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import com.partsoft.umsp.Context;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.packet.PacketInputStream;
import com.partsoft.umsp.sgip.Submit;
import com.partsoft.utils.CompareUtils;

public class SgipRequest implements com.partsoft.umsp.Request {
	
	/**
	 * @brief 序列号参数
	 */
	public static final String ARG_SEQUENCE = "sgip.context.sequence";
	
	/**
	 * @brief 已提交处理的消息列表
	 */
	public static final String ARG_SUBMIT_QUEUE = "sgip.context.submit.queue";

	/**
	 * @brief 已提交服务器的消息列表
	 */
	public static final String ARG_SUBMITTED_LIST = "sgip.submitted.list";

	/**
	 * @brief 已提交服务器的消息总数
	 */
	public static final String ARG_SUBMITTED_TOTAL = "sgip.submitted.total";

	/**
	 * @brief 已提交服务器并收到返回消息的总数
	 */
	public static final String ARG_SUBMITTED_RESULTS = "sgip.post.results";


	private com.partsoft.umsp.Request _request;

	private SgipDataPacket _dataPacket;

	private PacketInputStream _packetinput;

	public SgipRequest(com.partsoft.umsp.Request request) {
		super();
		this._request = request instanceof SgipRequest ? ((SgipRequest) request)._request : request;
	}

	protected void setWrapper(com.partsoft.umsp.Request request) {
		this._request = request;
	}

	public Request getWrapper() {
		return this._request;
	}

	protected void setDataPacket(SgipDataPacket _SGIPPacket) {
		this._dataPacket = _SGIPPacket;
	}

	public SgipDataPacket getDataPacket() {
		return _dataPacket;
	}

	public long getStartTimestamp() {
		return _request.getStartTimestamp();
	}

	public long getRequestTimestamp() {
		return _request.getRequestTimestamp();
	}

	public Object getAttribute(String paramString) {
		return _request.getAttribute(paramString);
	}

	public void setAttribute(String paramString, Object paramObject) {
		_request.setAttribute(paramString, paramObject);
	}

	public void removeAttribute(String paramString) {
		_request.removeAttribute(paramString);
	}

	public Enumeration<String> getAttributeNames() {
		return _request.getAttributeNames();
	}

	public int getContentLength() {
		return _request.getContentLength();
	}

	public InputStream getInputStream() throws IOException {
		return _request.getInputStream();
	}

	public PacketInputStream getPacketInputStream() throws IOException {
		if (_packetinput == null) {
			_packetinput = new PacketInputStream(_request.getInputStream());
		}
		return _packetinput;
	}

	public String getProtocol() {
		return _request.getProtocol();
	}

	public String getOriginName() {
		return _request.getOriginName();
	}

	public int getServerPort() {
		return _request.getServerPort();
	}

	public String getRemoteAddr() {
		return _request.getRemoteAddr();
	}

	public String getRemoteHost() {
		return _request.getRemoteHost();
	}

	public int getRemotePort() {
		return _request.getRemotePort();
	}

	public String getLocalName() {
		return _request.getLocalName();
	}

	public String getLocalAddr() {
		return _request.getLocalAddr();
	}

	public int getLocalPort() {
		return _request.getLocalPort();
	}

	public Context getContext() {
		return _request.getContext();
	}

	public boolean isSecure() {
		return _request.isSecure();
	}

	public boolean isHandled() {
		return _request.isHandled();
	}

	public boolean isBinded() {
		Object value = getAttribute(SgipContextHandler.ARG_BINDED);
		return CompareUtils.nullSafeEquals(value, Boolean.TRUE);
	}

	protected void setBinded(boolean binded) {
		if (binded) {
			setAttribute(SgipContextHandler.ARG_BINDED, Boolean.TRUE);
		} else {
			removeAttribute(SgipContextHandler.ARG_BINDED);
		}
	}

	public int getRequests() {
		return _request.getRequests();
	}
	
	/**
	 * 产生SEQ
	 * 
	 * @return
	 */
	public int generateSequence() {
		int result = 1;
		Context context = getContext();
		synchronized (context) {
			Integer seq = (Integer) context.getAttribute(ARG_SEQUENCE);
			if (seq == null) {
				seq = 1;
			}
			seq++;
			if (seq++ >= Integer.MAX_VALUE) {
				seq = 1;
			}
			result = seq;
			getContext().setAttribute(ARG_SEQUENCE, seq);
		}
		return result;
	}
	
	/**
	 * 获得提交返回总数
	 * 
	 * @return
	 */
	public int getSubmittedResultCount() {
		Integer result = (Integer) getAttribute(ARG_SUBMITTED_RESULTS);
		return result == null ? 0 : result < 0 ? 0 : result;
	}

	/**
	 * 判断是否存在提交的submit请求但尚未返回
	 * 
	 * @return
	 */
	public boolean isSubmiting() {
		boolean result = getSubmittedCount() > 0;
		return result;
	}
	
	/**
	 * 判断是否有提交在队列中
	 * @return
	 */
	public boolean isSubmitQueued() {
		return getContextSubmitQueue(getContext()).size() > 0;
	}
	

	/**
	 * 获得已提交服务器的消息个数
	 * 
	 * @return
	 */
	public int getSubmittedCount() {
		Integer result = (Integer) getAttribute(ARG_SUBMITTED_TOTAL);
		return result == null ? 0 : result < 0 ? 0 : result;
	}

	/**
	 * 获得已提交服务器的消息列表 (包括尚未提交完成的)
	 * 
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Submit> getSubmitteds() {
		List<Submit> result = (List<Submit>) getAttribute(ARG_SUBMITTED_LIST);
		result = result == null ? Collections.EMPTY_LIST : result;
		return result;
	}

	/**
	 * 获得待发消息队列
	 * 
	 * @param ctx
	 *            上下文
	 * @return
	 */
	static Queue<Submit> getContextSubmitQueue(Context ctx) {
		synchronized (ctx) {
			@SuppressWarnings("unchecked")
			Queue<Submit> submits = (Queue<Submit>) ctx.getAttribute(ARG_SUBMIT_QUEUE);
			if (submits == null) {
				submits = new LinkedBlockingDeque<Submit>();
				ctx.setAttribute(ARG_SUBMIT_QUEUE, submits);
			}
			return submits;
		}
	}

	/**
	 * 从待发队列中获取指定数量的消息列表
	 * 
	 * @param ctx
	 *            上下文
	 * @param takeCount
	 *            提取数量
	 * @return 固定大小的数组列表
	 */
	public List<Submit> takeSubmits(int takeCount) {
		Queue<Submit> submit_queue = getContextSubmitQueue(getContext());
		List<Submit> submit_result = new ArrayList<Submit>(takeCount);
		synchronized (submit_queue) {
			do {
				Submit submit = submit_queue.poll();
				if (submit != null) {
					submit_result.add(submit);
					if (submit_result.size() < takeCount) {
						continue;
					}
				}
			} while (false);
		}
		return submit_result;
	}

	public void cleanSubmitteds() {
		removeAttribute(ARG_SUBMITTED_LIST);
		removeAttribute(ARG_SUBMITTED_TOTAL);
		removeAttribute(ARG_SUBMITTED_RESULTS);
	}

	public void updateSubmittedResultCount(int count) {
		synchronized (this) {
			if (isSubmiting()) {
				setAttribute(ARG_SUBMITTED_RESULTS, count);
			}
		}
	}

	/**
	 * 压入提交请求
	 * 
	 * @param submits
	 */
	public void pushSubmits(List<Submit> submits, int from, int length) {
		Queue<Submit> submit_queue = getContextSubmitQueue(getContext());
		synchronized (submit_queue) {
			from = from < 0 ? 0 : from;
			if (from >= submits.size()) {
				from = submits.size() - 1;
			}
			int maxLength = (from + length);
			if (maxLength > submits.size()) {
				maxLength = submits.size();
			}

			for (int i = from; i < (maxLength); i++) {
				Submit submit = submits.get(i);
				if (submit != null) {
					submit_queue.add(submit);
				}
			}
		}
	}

	/**
	 * 在上下文中压入提交
	 * 
	 * @param ctx
	 * @param submits
	 */
	public static void pushSubmits(Context ctx, List<Submit> submits, int from, int length) {
		Queue<Submit> queue = getContextSubmitQueue(ctx);
		synchronized (queue) {
			from = from < 0 ? 0 : from;
			if (from >= submits.size()) {
				from = submits.size() - 1;
			}
			int maxLength = (from + length);
			if (maxLength > submits.size()) {
				maxLength = submits.size();
			}
			for (int i = from; i < (maxLength); i++) {
				Submit submit = submits.get(i);
				if (submit != null) {
					queue.offer(submit);
				}
			}
		}
	}

	/**
	 * 在上下文中压入提交
	 * 
	 * @param ctx
	 * @param submits
	 */
	static void pushSubmits(Context ctx, Collection<Submit> submits) {
		Queue<Submit> queue = getContextSubmitQueue(ctx);
		synchronized (queue) {
			for (Submit submit : submits) {
				if (submit != null) {
					queue.offer(submit);
				}
			}
		}
	}

	/**
	 * 把已发送至服务器的消息列表保存快照
	 * 
	 * @param list
	 *            列表
	 * @param total
	 *            总数
	 */
	public void updateSubmitteds(List<Submit> list, int total) {
		synchronized (this) {
			List<Submit> posts = getSubmitteds();
			if (posts != list) {
				setAttribute(ARG_SUBMITTED_LIST, list);
			}
			setAttribute(ARG_SUBMITTED_TOTAL, total);
		}
	}
}