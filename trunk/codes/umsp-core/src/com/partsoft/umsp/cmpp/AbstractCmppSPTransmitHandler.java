package com.partsoft.umsp.cmpp;

import java.io.IOException;
import java.util.List;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.handler.TransmitEvent;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.cmpp.Submit;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public abstract class AbstractCmppSPTransmitHandler extends AbstractCmppContextSPHandler {

	private boolean retrySubmit = false;

	/**
	 * 链路测试间隔时间
	 */
	protected long _activeTestIntervalTime = 1000 * 60 * 3;

	/**
	 * @brief 最大链路测试数量
	 */
	protected int _maxActiveTestCount = 3;

	/**
	 * @brief 一次最多发送16条 （默认参数）
	 */
	protected int _maxOnceSubmits = 16;

	/**
	 * 最大每秒提交数
	 */
	protected int maxSubmitPerSecond = 50;

	/**
	 * 发生错误时是否返回队列
	 */
	protected boolean errorReturnQueue = false;
	
	/**
	 * 确认是否返回度列
	 */

	protected Object transmitListener;

	public AbstractCmppSPTransmitHandler() {
	}
	
	public void setErrorReturnQueue(boolean errorReturnQueue) {
		this.errorReturnQueue = errorReturnQueue;
	}

	public void setTransmitListener(TransmitListener listener) {
		if (listener == null) {
			this.transmitListener = null;
		} else {
			this.transmitListener = ListUtils.add(this.transmitListener, listener);
		}
	}

	public void setMaxSubmitPerSecond(int maxSubmitPerSecond) {
		this.maxSubmitPerSecond = maxSubmitPerSecond;
	}

	public void addTransmitListener(TransmitListener listener) {
		this.transmitListener = ListUtils.add(this.transmitListener, listener);
	}

	public void removeTransmitListener(TransmitListener listener) {
		this.transmitListener = ListUtils.remove(this.transmitListener, listener);
	}

	public long getActiveTestIntervalTime() {
		return _activeTestIntervalTime;
	}

	public void setActiveTestIntervalTime(long activeTestIntervalTime) {
		this._activeTestIntervalTime = activeTestIntervalTime;
	}

	public void setMaxOnceSubmits(int maxOnceSubmits) {
		this._maxOnceSubmits = maxOnceSubmits;
	}

	public int getMaxOnceSubmits() {
		return _maxOnceSubmits;
	}

	public int getMaxActiveTestCount() {
		return _maxActiveTestCount;
	}

	public void setMaxActiveTestCount(int maxActiveTestCount) {
		this._maxActiveTestCount = maxActiveTestCount;
	}

	/**
	 * 返还排队发送的数据
	 * 
	 * @param submits
	 *            等于null或者size=0表示全面提取的任务已完成。
	 */
	protected abstract void returnQueuedSubmits(List<Submit> submits);

	/**
	 * 提取排队发送的数据
	 * 
	 * @return
	 */
	protected abstract List<Submit> takeQueuedSubmits();

	/**
	 * 判断是否有排队的数据
	 * 
	 * @return
	 */
	protected abstract boolean testQueuedSubmits();

	/**
	 * 接收消息
	 * 
	 * @param deliver
	 */
	protected abstract void doReceivedMessage(Deliver deliver);

	/**
	 * 接收报告
	 * 
	 * @param report
	 */
	protected abstract void doReceivedReport(Deliver report);

	@SuppressWarnings("unchecked")
	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		long request_idle_time = System.currentTimeMillis() - request.getRequestTimestamp();
		boolean request_submiting = CmppUtils.testRequestSubmiting(request);
		int request_activetest_count = CmppUtils.extractRequestActiveTestCount(request);
		boolean throw_packet_timeout = false;
		
		if (CmppUtils.testRequestBinded(request) && request_activetest_count < getMaxActiveTestCount()) {
			if (request_idle_time >= this._activeTestIntervalTime * (request_activetest_count + 1)) {
				doActiveTestRequest(request, response);
			} else if (request_activetest_count <= 0 && !request_submiting && testQueuedSubmits()) {
				doPostSubmit(request, response);
			}
		} else if (CmppUtils.testRequestBinding(request)) {
			throw_packet_timeout = request_idle_time >= this._activeTestIntervalTime;
		} else {
			throw_packet_timeout = true;
		}
		if (throw_packet_timeout) {
			if (request_submiting) {
				int submitted_result_count = CmppUtils.extractRequestSubmittedRepliedCount(request);
				int submitted_count = CmppUtils.extractRequestSubmittedCount(request);
				Log.warn("For a long time did not receive a reply, return submit to queue, submit-count=" + submitted_count + ", reply-count=" + submitted_count);
				List<Submit> submitted_list = (List<Submit>) CmppUtils.extractRequestSubmitteds(request);
				List<Submit> unresult_list = submitted_list.subList(submitted_result_count, submitted_count);
				if (this.errorReturnQueue) {
					returnQueuedSubmits(unresult_list);
				}
				CmppUtils.cleanRequestSubmitteds(request);
				int transmit_listener_size = ListUtils.size(transmitListener);
				if (transmit_listener_size > 0 && unresult_list.size() > 0) {
					for (int ii = 0; ii < unresult_list.size(); ii++) {
						TransmitEvent event = new TransmitEvent(unresult_list.get(ii));
						for (int i = 0; i < transmit_listener_size; i++) {
							TransmitListener listener = (TransmitListener) ListUtils.get(transmitListener, i);
							listener.transmitTimeout(event);
						}
					}
				}
			}
			throw new PacketException(String.format(request_activetest_count + " times active test, but not reply"));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleDisConnect(Request request, Response response) {
		if (CmppUtils.testRequestBinded(request) && CmppUtils.testRequestSubmiting(request) && this.errorReturnQueue) {
			int submitted_result_count = CmppUtils.extractRequestSubmittedRepliedCount(request);
			int submitted_count = CmppUtils.extractRequestSubmittedCount(request);
			Log.warn("return submitted to queue, submit-count=" + submitted_count + ", reply-count=" + submitted_count);
			List<Submit> submitted_list = (List<Submit>) CmppUtils.extractRequestSubmitteds(request);
			returnQueuedSubmits(submitted_list.subList(submitted_result_count, submitted_count));
			CmppUtils.cleanRequestSubmitteds(request);
		}
		super.handleDisConnect(request, response);
	}

	protected void doPostSubmit(Request request, Response response) throws IOException {
		// 上次流量统计的开始时间
		long flowLastTime = CmppUtils.extractRequestFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = CmppUtils.extractRequestFlowTotal(request);
		// 当前时间
		long currentTimeMilles = System.currentTimeMillis();
		if (Log.isDebugEnabled()) {
			Log.info(String.format("time=%d, interal=%d, total=%d ", currentTimeMilles,
					(currentTimeMilles - flowLastTime), flowTotal));
		}
		// 如果间隔小于1秒和发送总数大于
		if ((currentTimeMilles - flowLastTime) < 1000 && flowTotal >= this.maxSubmitPerSecond) {
			return;
		} else if ((currentTimeMilles - flowLastTime) >= 1000) {
			flowLastTime = currentTimeMilles;
			flowTotal = 0;
		}

		// TODO 需要实现发送速度控制
		String sp_number = Integer.toString(this.spNumber);
		Integer submitted = Integer.valueOf(0);
		List<Submit> takedPostSubmits = null;
		try {
			takedPostSubmits = takeQueuedSubmits();
		} catch (Throwable e) {
			Log.error("take submit from queue error: " + e.getMessage(), e);
		}
		if (takedPostSubmits != null) {
			for (Submit sb : takedPostSubmits) {
				sb.spId = "" + this.enterpriseId;
				if (!StringUtils.hasText(sb.sourceId) || !sb.sourceId.startsWith(sp_number)) {
					sb.sourceId = "" + this.spNumber;
				}
//				sb.nodeId = this.enterpriseId;
//				sb.nodeTime = CalendarUtils.getTimestampInYearDuring(sb.createTimeMillis);
				sb.sequenceId = CmppUtils.generateRequestSequence(request);
//				sb.nodeSeq = sb.sequenceId;
				sb.protocolVersion = this.protocolVersion;
				int transmit_listener_size = ListUtils.size(transmitListener);
				if (transmit_listener_size > 0) {
					TransmitEvent event = new TransmitEvent(sb);
					for (int j = 0; j < transmit_listener_size; j++) {
						TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
						try {
							evnListener.beginTransmit(event);
						} catch (Throwable e) {
							Log.ignore(e);
						}
					}
				}
				try {
					CmppUtils.renderDataPacket(request, response, sb);
					response.flushBuffer();
					CmppUtils.updateSubmitteds(request, takedPostSubmits, submitted + 1);
					flowTotal++;
					CmppUtils.updateRequestFlowTotal(request, flowLastTime, flowTotal);
				} catch (IOException e) {
					if (this.errorReturnQueue) {
						returnQueuedSubmits(takedPostSubmits);
					} else {
						returnQueuedSubmits(takedPostSubmits.subList(submitted, takedPostSubmits.size()));
					}
					CmppUtils.cleanRequestSubmitteds(request);
					throw e;
				}
				submitted++;
				transmit_listener_size = ListUtils.size(transmitListener);
				if (transmit_listener_size > 0) {
					TransmitEvent event = new TransmitEvent(sb);
					for (int j = 0; j < transmit_listener_size; j++) {
						TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
						try {
							evnListener.transmitted(event);
						} catch (Throwable e) {
							Log.ignore(e);
						}
					}
				}
			}
		}
	}

	@Override
	protected void doDeliver(Request request, Response response) throws IOException {
		super.doDeliver(request, response);
		Deliver deliver_packet = (Deliver) CmppUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}

		int result = 0;
		try {
			if (deliver_packet.registeredDelivery != 1) {
				doReceivedMessage(deliver_packet);
			} else {
				doReceivedReport(deliver_packet);
			}
		} catch (Throwable e) {
			Log.error(e.getMessage(), e);
			result = 9;
		}

		DeliverResponse deliver_response = new DeliverResponse();
		
		//必须相同
		deliver_response.sequenceId = deliver_packet.sequenceId;
		
		// 构建msgid
		deliver_response.nodeId = deliver_packet.nodeId;
		deliver_response.nodeTime = deliver_packet.nodeTime;
		deliver_response.nodeSeq = deliver_packet.nodeSeq;

		deliver_response.result = result;
		CmppUtils.renderDataPacket(request, response, deliver_response);
		response.flushBuffer();
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		CmppUtils.setupRequestBinding(request, true);
		Connect login = new Connect();
		login.timestamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
		login.enterpriseId = this.account;
		login.authenticationToken = CmppUtils.generateClientToken(login.enterpriseId, password, login.timestamp);
		login.sequenceId = CmppUtils.generateRequestSequence(request);
		CmppUtils.renderDataPacket(request, response, login);
		response.flushBuffer();
	}

	@SuppressWarnings("unchecked")
	protected void doSubmitResult(Request request, Response response) throws IOException {
		SubmitResponse res = (SubmitResponse) CmppUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}

		boolean do_unbind = false;
		if (CmppUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = CmppUtils.extractRequestSubmittedRepliedCount(request);
			List<Submit> posts = (List<Submit>) CmppUtils.extractRequestSubmitteds(request);
			CmppUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
			Submit submitted = posts.get(last_pare_submit_index);
			int transmit_listener_size = ListUtils.size(transmitListener);
			
			//TODO 临时日志
			//Log.info("submit replied|" + CmppUtils.extractRequestSubmittedRepliedCount(request) + ", " + CmppUtils.extractRequestSubmittedCount(request));
			if (transmit_listener_size > 0) {
				TransmitEvent event = new TransmitEvent(new Object[] { submitted, res });
				for (int j = 0; j < ListUtils.size(transmitListener); j++) {
					TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
					try {
						evnListener.endTransmit(event);
					} catch (Throwable e) {
						Log.ignore(e);
					}
				}
			}
			
			if (CmppUtils.extractRequestSubmittedRepliedCount(request) >= CmppUtils
					.extractRequestSubmittedCount(request)) {
				//Log.info("batch submitted success |" + CmppUtils.extractRequestSubmittedRepliedCount(request) + ", " + CmppUtils.extractRequestSubmittedCount(request));
				returnQueuedSubmits(null);
				CmppUtils.cleanRequestSubmitteds(request);
				do_unbind = isAutoReSubmit() ? false : true;
				if (testQueuedSubmits()) {
					this.doPostSubmit(request, response);
				} else {
					do_unbind = isAutoReSubmit() ? false : true;
				}
			}
		} else {
			Log.warn("not found submitted, but receive submit reply???");
			do_unbind = isAutoReSubmit() ? false : true;
		}

		if (do_unbind) {
			Terminate unbind = new Terminate();
			unbind.sequenceId = CmppUtils.generateRequestSequence(request);
			CmppUtils.renderDataPacket(request, response, unbind);
			response.finalBuffer();
		}
	}

	@Override
	protected void doSubmitResponse(Request request, Response response) throws IOException {
		super.doSubmitResponse(request, response);
		doSubmitResult(request, response);
	}

	@Override
	protected void doBindResponse(Request request, Response response) throws IOException {
		CmppUtils.setupRequestBinding(request, false);
		ConnectResponse res = (ConnectResponse) CmppUtils.extractRequestPacket(request);
		if (res.status == 0) {
			CmppUtils.setupRequestBinded(request, true);
			CmppUtils.updateRequestProtocolVersion(request, res.protocolVersion);
			if (testQueuedSubmits()) {
				doPostSubmit(request, response);
			}
		} else {
			throw new ConnectException("login error, please check user or password or validate your ip address, response: " + res.toString());
		}
	}

	//@SuppressWarnings("unchecked")
	@Override
	protected void doActiveTest(Request request, Response response) throws IOException {
		super.doActiveTest(request, response);
		//TODO 临时日志
		//Log.warn("收到来自服务器端的链路侦测包");
		/*
		if (CmppUtils.testRequestSubmiting(request)) {
			int submitted_result_count = CmppUtils.extractRequestSubmittedRepliedCount(request);
			int submitted_count = CmppUtils.extractRequestSubmittedCount(request);
			Log.warn("Submit not reply but active test receive, submit-count=" + submitted_count + ", reply-count=" + submitted_count);
			
			List<Submit> submitted_list = (List<Submit>) CmppUtils.extractRequestSubmitteds(request);
			returnQueuedSubmits(submitted_list.subList(submitted_result_count, submitted_count));
			CmppUtils.cleanRequestSubmitteds(request);
		} else if (testQueuedSubmits()) {
			doPostSubmit(request, response);
		}
		*/
		if (!CmppUtils.testRequestSubmiting(request) && testQueuedSubmits()) {
			doPostSubmit(request, response);
		}
	}

	@Override
	protected void doActiveTestResponse(Request request, Response response) throws IOException {
		super.doActiveTestResponse(request, response);
		//TODO 临时日志
		//Log.warn("发送置服务端的链路侦测包已应答");
		if (!CmppUtils.testRequestSubmiting(request) && testQueuedSubmits()) {
			doPostSubmit(request, response);
		}
	}

	@Override
	public void setOrigin(OriginHandler server) {
		super.setOrigin(server);
		if (server != null && server instanceof Client) {
			retrySubmit = ((Client) server).isAutoReConnect();
		}
	}

	public boolean isAutoReSubmit() {
		return retrySubmit;
	}

}
