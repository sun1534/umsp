package com.partsoft.umsp.smgp;

import java.io.IOException;
import java.util.List;
import java.util.Queue;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.Context;
import com.partsoft.umsp.LifeCycle;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.smgp.Submit;
import com.partsoft.umsp.smgp.Constants.LoginModes;
import com.partsoft.umsp.smgp.Constants.StatusCodes;
import com.partsoft.utils.Assert;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;

public class SmgpContextSPSHandler extends SmgpContextSPHandler {

	private List<Submit> _delayContextSubmits;

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

	protected Object postSubmitListener;

	private LifeCycle.Listener client_listener = new LifeCycle.Listener() {

		public void lifeCycleStopping(LifeCycle event) {
			if (event instanceof Client && event.equals(getOrigin())) {
				Client client = (Client) event;
				Queue<Submit> submits = SmgpRequest.getContextSubmitQueue(getContext());
				if (submits.size() > 0) {
					RuntimeException tex = new IllegalArgumentException(String.format("%d unfinished post Submit",
							submits.size()));
					if (retrySubmit) {
						Log.error(tex);
					} else {
						client.pushDelayException(tex);
					}
				}
			}
		}

		public void lifeCycleStopped(LifeCycle event) {

		}

		public void lifeCycleStarting(LifeCycle event) {
		}

		public void lifeCycleStarted(LifeCycle event) {
		}

		public void lifeCycleFailure(LifeCycle event, Throwable cause) {
		}
	};

	public SmgpContextSPSHandler() {
	}

	public void setLoginMode(int loginMode) {
		this._loginMode = loginMode;
	}

	public void setPostSubmitListener(PostSubmitListener listener) {
		this.postSubmitListener = ListUtils.add(this.postSubmitListener, listener);
	}

	public void addSubmitListener(PostSubmitListener listener) {
		this.postSubmitListener = ListUtils.add(this.postSubmitListener, listener);
	}

	public void removeSubmitListener(PostSubmitListener listener) {
		this.postSubmitListener = ListUtils.remove(this.postSubmitListener, listener);
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

	@Override
	protected void handleContextInitialized(Context ctx) {
		super.handleContextInitialized(ctx);
		if (_delayContextSubmits != null) {
			SmgpRequest.pushSubmits(ctx, _delayContextSubmits);
		}
	}

	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		SmgpRequest smgp_request = getSmgpRequest(request, response);
		SmgpResponse smgp_response = getSmgpResponse(request, response);
		if (smgp_request.isBinded() && smgp_request.getActiveTestingCount() < getMaxActiveTestCount()) {
			long request_idle_time = System.currentTimeMillis() - smgp_request.getRequestTimestamp();
			boolean is_active_testing = smgp_request.isActiveTesting();
			if (is_active_testing || request_idle_time > _activeTestIntervalTime) {
				if (smgp_request.isSubmiting()) {
					int submitted_result_count = smgp_request.getSubmittedResultCount();
					int submitted_count = smgp_request.getSubmittedCount();
					smgp_request.pushSubmits(smgp_request.getSubmitteds(), submitted_result_count, submitted_count
							- submitted_result_count);
					smgp_request.cleanSubmitteds();
				}
				doActiveTestRequest(smgp_request, smgp_response);
				smgp_request.stepActiveTesting();
			}
		} else {
			super.handleTimeout(smgp_request, smgp_response);
		}
	}

	protected void doPostSubmit(SmgpRequest request, SmgpResponse response) throws IOException {
		Integer submitted = Integer.valueOf(0);
		List<Submit> takedPostSubmits = request.takeSubmits(getMaxOnceSubmits());
		for (Submit sb : takedPostSubmits) {
			sb.SrcTermID = "" + spNumber;
			sb.MsgType = 6;
			sb.sequenceId = request.generateSequence();
			for (int j = 0; j < ListUtils.size(postSubmitListener); j++) {
				PostSubmitListener evnListener = (PostSubmitListener) ListUtils.get(postSubmitListener, j);
				try {
					evnListener.onBeforePost(new PostSubmitEvent(sb));
				} catch (Throwable e) {
					Log.ignore(e);
				}
			}
			try {
				response.flushDataPacket(sb);
				request.updateSubmitteds(takedPostSubmits, submitted + 1);
			} catch (IOException e) {
				request.cleanSubmitteds();
				// 为可靠起见，把所有提取的submit都回退至队列中
				// TODO 考虑设置一个参数，可以把已经提交的不回退
				request.pushSubmits(takedPostSubmits, 0, takedPostSubmits.size());
				throw e;
			}
			submitted++;
			for (int j = 0; j < ListUtils.size(postSubmitListener); j++) {
				PostSubmitListener evnListener = (PostSubmitListener) ListUtils.get(postSubmitListener, j);
				try {
					evnListener.onPostSubmit(new PostSubmitEvent(sb));
				} catch (Throwable e) {
					Log.ignore(e);
				}
			}
		}
	}

	@Override
	protected void doDeliver(SmgpRequest request, SmgpResponse response) throws IOException {
		super.doDeliver(request, response);
		Deliver deliver_packet = (Deliver) request.getDataPacket();
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}
		DeliverResponse deliver_response = new DeliverResponse();
		deliver_response.sequenceId = deliver_packet.sequenceId;
		
		//设置MSGID
		deliver_response.NodeId = deliver_packet.NodeId;
		deliver_response.NodeTime = deliver_packet.NodeTime;
		deliver_response.NodeSequenceId = deliver_packet.NodeSequenceId;
		
		deliver_response.Status = StatusCodes.ERR_SUCCESS;
		response.flushDataPacket(deliver_response);
	}

	/**
	 * 提交发送消息
	 * 
	 * @param submits
	 */
	public void postSubmit(List<Submit> submits, int from, int length) {
		Assert.isTrue(_loginMode == LoginModes.SEND || _loginMode == LoginModes.TRANSMIT, "not support submit");
		if (isStarted()) {
			SmgpRequest.pushSubmits(getContext(), submits, from, length);
		} else {
			_delayContextSubmits = submits.subList(from, from + length);
		}
	}

	@Override
	protected void doBindRequest(SmgpRequest request, SmgpResponse response) throws IOException {
		Login login = new Login();
		login.TimeStamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
		login.ClientID = account;
		login.AuthenticatorClient = SmgpUtils.generateClientToken(login.ClientID, password, login.TimeStamp);
		login.LoginMode = this._loginMode;
		login.sequenceId = request.generateSequence();
		response.flushDataPacket(login);
	}

	protected void doSubmitResult(SmgpRequest request, SmgpResponse response) throws IOException {
		SubmitResponse res = (SubmitResponse) request.getDataPacket();
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}

		boolean do_unbind = false;

		if (request.isSubmiting()) {
			int last_pare_submit_index = request.getSubmittedResultCount();
			List<Submit> posts = request.getSubmitteds();
			request.updateSubmittedResultCount(last_pare_submit_index + 1);
			if (request.getSubmittedResultCount() == request.getSubmittedCount()) {
				request.cleanSubmitteds();
				if (request.isSubmitQueued()) {
					this.doPostSubmit(request, response);
				} else {
					do_unbind = isAutoReSubmit() ? false : true;
				}
			}
			Submit submitted = posts.get(last_pare_submit_index);
			for (int j = 0; j < ListUtils.size(postSubmitListener); j++) {
				PostSubmitListener evnListener = (PostSubmitListener) ListUtils.get(postSubmitListener, j);
				try {
					evnListener.onSubmitResponse(new SubmitResponseEvent(submitted, res));
				} catch (Throwable e) {
					Log.ignore(e);
				}
			}
		} else {
			do_unbind = isAutoReSubmit() ? false : true;
		}

		if (do_unbind) {
			Exit unbind = new Exit();
			unbind.sequenceId = request.generateSequence();
			response.writeDataPacket(unbind);
			response.finalBuffer();
		}
	}

	@Override
	protected void doSubmitResponse(SmgpRequest request, SmgpResponse response) throws IOException {
		super.doSubmitResponse(request, response);
		doSubmitResult(request, response);
	}

	@Override
	protected void doBindResponse(SmgpRequest request, SmgpResponse response) throws IOException {
		LoginResponse res = (LoginResponse) request.getDataPacket();
		if (res.Status == StatusCodes.ERR_SUCCESS) {
			request.setBinded(true);
			request.updateProtocolVersion(res.Version);
			doPostSubmit(request, response);
		} else {
			throw new LoginException(
					"login error, please check user or password or validate your ip address, response: "
							+ res.toString());
		}
	}

	@Override
	protected void doActiveTest(SmgpRequest request, SmgpResponse response) throws IOException {
		super.doActiveTest(request, response);
		if (!request.isSubmiting()) {
			doPostSubmit(request, response);
		} else {
			int submitted_result_count = request.getSubmittedResultCount();
			int submitted_count = request.getSubmittedCount();
			request.pushSubmits(request.getSubmitteds(), submitted_result_count, submitted_count
					- submitted_result_count);
			request.cleanSubmitteds();
			throw new SmgpException("Cann't receive submit response.");
		}
	}

	@Override
	protected void doActiveTestResponse(SmgpRequest request, SmgpResponse response) throws IOException {
		super.doActiveTestResponse(request, response);
		doPostSubmit(request, response);
	}

	@Override
	protected void doActiveTestRequest(SmgpRequest request, SmgpResponse response) throws IOException {
		super.doActiveTestRequest(request, response);
	}

	@Override
	public void setOrigin(OriginHandler server) {
		OriginHandler old_server = getOrigin();
		if (old_server != null) {
			old_server.removeLifeCycleListener(client_listener);
		}
		super.setOrigin(server);
		if (server != null && server instanceof Client) {
			retrySubmit = ((Client) server).isAutoReConnect();
		}
		if (server != null) {
			server.addLifeCycleListener(client_listener);
		}
	}

	public boolean isAutoReSubmit() {
		return retrySubmit;
	}

}
