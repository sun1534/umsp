package com.partsoft.umsp.smgp;

import java.io.IOException;
import java.util.List;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.handler.TransmitEvent;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.sgip.SgipUtils;
import com.partsoft.umsp.smgp.Submit;
import com.partsoft.umsp.smgp.Constants.LoginModes;
import com.partsoft.umsp.smgp.Constants.StatusCodes;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

/**
 * 电信协议用于收发双向通道的抽象实现
 * 
 * @author neeker
 */
public abstract class AbstractSmgpSPTransmitHandler extends AbstractSmgpContextSPHandler {

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
	 * @brief 一次最多发送16条
	 */
	protected int _maxOnceSubmits = 16;

	protected int _loginMode = LoginModes.TRANSMIT;

	protected Object transmitListener;

	public AbstractSmgpSPTransmitHandler() {
	}

	public void setLoginMode(int loginMode) {
		this._loginMode = loginMode;
	}

	public void setTransmitListener(TransmitListener listener) {
		if (listener == null) {
			this.transmitListener = null;
		} else {
			this.transmitListener = ListUtils.add(this.transmitListener, listener);
		}
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
	 * @param from
	 * @param length
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

	/**
	 * 最大每秒提交数
	 */
	protected int maxSubmitPerSecond = 50;

	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		if (SmgpUtils.testRequestBinded(request)
				&& SmgpUtils.extractRequestActiveTestCount(request) < getMaxActiveTestCount()) {
			long request_idle_time = System.currentTimeMillis() - request.getRequestTimestamp();
			boolean is_active_testing = SmgpUtils.testRequestActiveTesting(request);
			if (is_active_testing || request_idle_time > _activeTestIntervalTime) {
				if (SmgpUtils.testRequestSubmiting(request)) {
					// 如果有submit再队列中，则回退。
					int submitted_result_count = SmgpUtils.extractRequestSubmittedRepliedCount(request);
					int submitted_count = SmgpUtils.extractRequestSubmittedCount(request);
					List<Submit> submitted_list = SmgpUtils.extractRequestSubmitteds(request);
					List<Submit> unresult_list = submitted_list.subList(submitted_result_count, submitted_count);
					returnQueuedSubmits(unresult_list);
					SmgpUtils.cleanRequestSubmitteds(request);
					int transmit_listener_size = ListUtils.size(transmitListener);
					if (transmit_listener_size > 0 && unresult_list.size() > 0) {
						TransmitEvent event = new TransmitEvent(unresult_list);
						for (int i = 0; i < transmit_listener_size; i++) {
							TransmitListener listener = (TransmitListener) ListUtils.get(transmitListener, i);
							listener.transmitTimeout(event);
						}
					}
				}
				doActiveTestRequest(request, response);
				SmgpUtils.stepIncreaseRequestActiveTest(request);
			} else if (testQueuedSubmits()) {
				doPostSubmit(request, response);
			}
		} else {
			super.handleTimeout(request, response);
		}
	}

	public void setMaxSubmitPerSecond(int maxSubmitPerSecond) {
		this.maxSubmitPerSecond = maxSubmitPerSecond;
	}

	@Override
	protected void handleDisConnect(Request request, Response response) {
		if (SmgpUtils.testRequestBinded(request)) {
			List<Submit> submitts = SmgpUtils.extractRequestSubmitteds(request);
			int submittedCount = SgipUtils.extractRequestSubmittedCount(request);
			int submittedResultCount = SgipUtils.extractRequestSubmittedRepliedCount(request);
			returnQueuedSubmits(submitts.subList(submittedResultCount, submittedCount));
		}
		super.handleDisConnect(request, response);
	}

	protected void doPostSubmit(Request request, Response response) throws IOException {

		// 上次流量统计的开始时间
		long flowLastTime = SmgpUtils.extractRequestFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = SmgpUtils.extractRequestFlowTotal(request);
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

		Integer submitted = Integer.valueOf(0);
		// String enterprise_id = Integer.toString(this.enterpriseId);
		String sp_numer = Integer.toString(this.spNumber);

		List<Submit> takedPostSubmits = takeQueuedSubmits();
		for (Submit sb : takedPostSubmits) {
			if (!StringUtils.hasText(sb.SrcTermID) || !sb.SrcTermID.startsWith(sp_numer)) {
				sb.SrcTermID = sp_numer;
			}
			sb.MsgType = 6;
			sb.sequenceId = SmgpUtils.generateRequestSequence(request);

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
				SmgpUtils.renderDataPacket(request, response, sb);
				response.flushBuffer();
				SmgpUtils.updateSubmitteds(request, takedPostSubmits, submitted + 1);
				flowTotal++;
				SmgpUtils.updateRequestFlowTotal(request, flowLastTime, flowTotal);
			} catch (IOException e) {
				// 为可靠起见，把所有提取的submit都回退至队列中
				// TODO 考虑设置一个参数，可以把已经提交的不回退
				SmgpUtils.cleanRequestSubmitteds(request);
				returnQueuedSubmits(takedPostSubmits);
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

	@Override
	protected void doDeliver(Request request, Response response) throws IOException {
		super.doDeliver(request, response);
		Deliver deliver_packet = (Deliver) SmgpUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}
		int responseStatus = StatusCodes.ERR_SUCCESS;
		try {
			if (deliver_packet.IsReport != 1) {
				doReceivedMessage(deliver_packet);
			} else {
				doReceivedReport(deliver_packet);
			}
		} catch (Throwable e) {
			responseStatus = 40;
			Log.error(e.getMessage(), e);
		}
		DeliverResponse deliver_response = new DeliverResponse();
		deliver_response.sequenceId = deliver_packet.sequenceId;
		// 设置MSGID
		deliver_response.NodeId = deliver_packet.NodeId;
		deliver_response.NodeTime = deliver_packet.NodeTime;
		deliver_response.NodeSequenceId = deliver_packet.NodeSequenceId;
		deliver_response.Status = responseStatus;
		SmgpUtils.renderDataPacket(request, response, deliver_response);
		response.flushBuffer();
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		Login login = new Login();
		login.TimeStamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
		login.ClientID = account;
		login.AuthenticatorClient = SmgpUtils.generateClientToken(login.ClientID, password, login.TimeStamp);
		login.LoginMode = this._loginMode;
		login.sequenceId = SmgpUtils.generateRequestSequence(request);
		SmgpUtils.renderDataPacket(request, response, login);
		response.flushBuffer();
	}

	protected void doSubmitResult(Request request, Response response) throws IOException {
		SubmitResponse res = (SubmitResponse) SmgpUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}
		boolean do_unbind = false;
		if (SmgpUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = SmgpUtils.extractRequestSubmittedRepliedCount(request);
			List<Submit> posts = SmgpUtils.extractRequestSubmitteds(request);
			SmgpUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
			if (SmgpUtils.extractRequestSubmittedRepliedCount(request) == SmgpUtils
					.extractRequestSubmittedCount(request)) {
				returnQueuedSubmits(null);
				SmgpUtils.cleanRequestSubmitteds(request);
				if (testQueuedSubmits()) {
					this.doPostSubmit(request, response);
				} else {
					do_unbind = isAutoReSubmit() ? false : true;
				}
			}
			Submit submitted = posts.get(last_pare_submit_index);

			int transmit_listener_size = ListUtils.size(transmitListener);
			if (transmit_listener_size > 0) {
				TransmitEvent event = new TransmitEvent(new Object[] { submitted, res });
				for (int j = 0; j < transmit_listener_size; j++) {
					TransmitListener evnListener = (TransmitListener) ListUtils.get(transmitListener, j);
					try {
						evnListener.endTransmit(event);
					} catch (Throwable e) {
						Log.ignore(e);
					}
				}
			}
		} else {
			do_unbind = isAutoReSubmit() ? false : true;
		}

		if (do_unbind) {
			Exit unbind = new Exit();
			unbind.sequenceId = SmgpUtils.generateRequestSequence(request);
			SmgpUtils.renderDataPacket(request, response, unbind);
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
		LoginResponse res = (LoginResponse) SmgpUtils.extractRequestPacket(request);
		if (res.Status == StatusCodes.ERR_SUCCESS) {
			SmgpUtils.setupRequestBinded(request, true);
			SmgpUtils.updateRequestProtocolVersion(request, res.Version);
			if (testQueuedSubmits()) {
				doPostSubmit(request, response);
			}
		} else {
			throw new LoginException(
					"login error, please check user or password or validate your ip address, response: "
							+ res.toString());
		}
	}

	@Override
	protected void doActiveTest(Request request, Response response) throws IOException {
		super.doActiveTest(request, response);
		if (!SmgpUtils.testRequestSubmiting(request)) {
			if (testQueuedSubmits()) {
				doPostSubmit(request, response);
			}
		} else {
			int submitted_result_count = SmgpUtils.extractRequestSubmittedRepliedCount(request);
			int submitted_count = SmgpUtils.extractRequestSubmittedCount(request);
			List<Submit> submitted_list = SmgpUtils.extractRequestSubmitteds(request);
			returnQueuedSubmits(submitted_list.subList(submitted_result_count, submitted_count));
			SmgpUtils.cleanRequestSubmitteds(request);
		}
	}

	@Override
	protected void doActiveTestResponse(Request request, Response response) throws IOException {
		super.doActiveTestResponse(request, response);
		if (testQueuedSubmits()) {
			doPostSubmit(request, response);
		}
	}

	@Override
	protected void doActiveTestRequest(Request request, Response response) throws IOException {
		super.doActiveTestRequest(request, response);
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
