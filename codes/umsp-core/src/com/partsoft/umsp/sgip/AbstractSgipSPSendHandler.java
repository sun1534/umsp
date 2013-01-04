package com.partsoft.umsp.sgip;

import java.io.IOException;
import java.util.List;

import com.partsoft.umsp.Client;
import com.partsoft.umsp.Context;
import com.partsoft.umsp.OriginHandler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.handler.TransmitEvent;
import com.partsoft.umsp.handler.TransmitListener;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.umsp.sgip.Constants.BindTypes;
import com.partsoft.umsp.sgip.Submit;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;
import com.partsoft.utils.StringUtils;

public abstract class AbstractSgipSPSendHandler extends AbstractSgipContextSPHandler {

	private boolean retrySubmit = false;

	protected int maxOnceSubmits = 32;

	protected int maxRequestIdleTime = 1000 * 60;

	protected Object transmitListener;

	protected int maxSubmitPerSecond = 50;

	public boolean isAutoReSubmit() {
		return retrySubmit;
	}

	public AbstractSgipSPSendHandler() {
	}

	public void setMaxRequestIdleTime(int maxRequestIdleTime) {
		this.maxRequestIdleTime = maxRequestIdleTime;
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
	}

	public void setMaxSubmitPerSecond(int maxSubmitPerSecond) {
		this.maxSubmitPerSecond = maxSubmitPerSecond;
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

	public void setMaxOnceSubmits(int maxOnceSubmits) {
		this.maxOnceSubmits = maxOnceSubmits;
	}

	public int getMaxOnceSubmits() {
		return maxOnceSubmits;
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

	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		if (SgipUtils.testRequestBinded(request)) {
			long request_idle_time = System.currentTimeMillis() - request.getRequestTimestamp();
			if (request_idle_time > maxRequestIdleTime) {
				if (SgipUtils.testRequestSubmiting(request)) {
					List<Submit> submits = SgipUtils.extractRequestSubmitteds(request);
					int submitted_result_count = SgipUtils.extractRequestSubmittedRepliedCount(request);
					int submitted_count = SgipUtils.extractRequestSubmittedCount(request);
					returnQueuedSubmits(submits.subList(submitted_result_count, submitted_count));
					SgipUtils.cleanRequestSubmitteds(request);
				}
				super.handleTimeout(request, response);
			}
		} else {
			super.handleTimeout(request, response);
		}
	}

	protected void doPostSubmit(Request request, Response response) throws IOException {
		// 上次流量统计的开始时间
		long flowLastTime = SgipUtils.extractRequestFlowLastTime(request);
		// 上次统计以来流量总数
		int flowTotal = SgipUtils.extractRequestFlowTotal(request);
		// 当前时间
		long currentTimeMilles = System.currentTimeMillis();
		if (Log.isDebugEnabled()) {
			Log.debug(String.format("time=%d, interal=%d, total=%d ", currentTimeMilles,
					(currentTimeMilles - flowLastTime), flowTotal));
		}
		// 如果间隔小于1秒和发送总数大于
		if ((currentTimeMilles - flowLastTime) < 1000 && flowTotal >= this.maxSubmitPerSecond) {
			return;
		} else if ((currentTimeMilles - flowLastTime) >= 1000) {
			flowLastTime = currentTimeMilles;
			flowTotal = 0;
		}

		int node_id = getNodeId();
		String enterprise_id = Integer.toString(this.enterpriseId);
		String sp_numer = Integer.toString(this.spNumber);
		Integer submitted = 0;
		List<Submit> takedPostSubmits = takeQueuedSubmits();
		if (takedPostSubmits != null) {
			for (Submit sb : takedPostSubmits) {
				if (!StringUtils.hasText(sb.sp_number) || !sb.sp_number.startsWith(sp_numer)) {
					sb.sp_number = sp_numer;
				}
				sb.corporation_id = enterprise_id;
				SgipUtils.stuffSerialNumber(sb, request, node_id, sb.createTimeMillis);
				sb.node_id = node_id;
				sb.timestamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
				sb.sequence = SgipUtils.generateRequestSequence(request);

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
					SgipUtils.renderDataPacket(request, response, sb);
					response.flushBuffer();
					SgipUtils.updateSubmitteds(request, takedPostSubmits, submitted + 1);
					flowTotal++;
					SgipUtils.updateRequestFlowTotal(request, flowLastTime, flowTotal);
				} catch (IOException e) {
					// 为可靠起见，把所有提取的submit都回退至队列中
					// TODO 考虑设置一个参数，可以把已经提交的不回退
					SgipUtils.cleanRequestSubmitteds(request);
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
	}

	protected void doSubmitResult(Request request, Response response) throws IOException {
		SubmitResponse res = (SubmitResponse) SgipUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(res.toString());
		}

		boolean do_unbind = false;

		if (SgipUtils.testRequestSubmiting(request)) {
			int last_pare_submit_index = SgipUtils.extractRequestSubmittedRepliedCount(request);
			List<Submit> posts = SgipUtils.extractRequestSubmitteds(request);
			SgipUtils.updateSubmittedRepliedCount(request, last_pare_submit_index + 1);
			if (SgipUtils.extractRequestSubmittedRepliedCount(request) == SgipUtils
					.extractRequestSubmittedCount(request)) {
				returnQueuedSubmits(null);
				SgipUtils.cleanRequestSubmitteds(request);
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
			UnBind unbind = new UnBind();
			SgipUtils.stuffSerialNumber(unbind, request, getNodeId(), unbind.createTimeMillis);
			SgipUtils.renderDataPacket(request, response, unbind);
			response.finalBuffer();
		}
	}

	@Override
	protected void doUnBindResponse(Request request, Response response) throws IOException {
		super.doUnBindResponse(request, response);
	}

	@Override
	protected void doBindResponse(Request request, Response response) throws IOException {
		BindResponse res = (BindResponse) SgipUtils.extractRequestPacket(request);
		if (res.result == BindResults.SUCCESS) {
			SgipUtils.setupRequestBinded(request, true);
			if (testQueuedSubmits()) {
				doPostSubmit(request, response);
			}
		} else {
			throw new BindException("bind error, please check user or password or  validate your ip address");
		}
	}

	@Override
	protected void doSubmitResponse(Request request, Response response) throws IOException {
		super.doSubmitResponse(request, response);
		doSubmitResult(request, response);
	}

	@Override
	protected void doBindRequest(Request request, Response response) throws IOException {
		Bind bind = new Bind();
		bind.user = account;
		bind.pwd = password;
		bind.type = BindTypes.SP_TO_SMG;

		SgipUtils.stuffSerialNumber(bind, request, getNodeId(), bind.createTimeMillis);
		SgipUtils.renderDataPacket(request, response, bind);
		response.flushBuffer();
	}

	@Override
	public void setOrigin(OriginHandler server) {
		super.setOrigin(server);
		if (server != null && server instanceof Client) {
			retrySubmit = ((Client) server).isAutoReConnect();
		}
	}

	@Override
	protected void handleContextInitialized(Context ctx) {
		super.handleContextInitialized(ctx);
	}

	@Override
	protected void handleContextDestroyed(Context ctx) {
		super.handleContextDestroyed(ctx);
	}

	@Override
	protected void handleDisConnect(Request request, Response response) {
		if (SgipUtils.testRequestBinded(request)) {
			List<Submit> submitts = SgipUtils.extractRequestSubmitteds(request);
			int submittedCount = SgipUtils.extractRequestSubmittedCount(request);
			int submittedResultCount = SgipUtils.extractRequestSubmittedRepliedCount(request);
			returnQueuedSubmits(submitts.subList(submittedResultCount, submittedCount));
		}
		super.handleDisConnect(request, response);
	}

}
