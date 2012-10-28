package com.partsoft.umsp.sgip;

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
import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.umsp.sgip.Constants.BindTypes;
import com.partsoft.umsp.sgip.Submit;
import com.partsoft.umsp.sgip.PostSubmitListener;
import com.partsoft.umsp.sgip.SubmitResponseEvent;
import com.partsoft.utils.CalendarUtils;
import com.partsoft.utils.ListUtils;

public class SgipContextSPCHandler extends SgipContextSPHandler {

	private List<Submit> _delayContextSubmits;

	private boolean retrySubmit = false;

	protected int maxOnceSubmits = 32;

	protected int maxRequestIdleTime = 1000 * 60;

	protected Object postSubmitListener;
	
	private LifeCycle.Listener client_listener = new LifeCycle.Listener() {

		public void lifeCycleStopping(LifeCycle event) {
			if (event instanceof Client && event.equals(getOrigin())) {
				Client client = (Client) event;
				Queue<Submit> submits = SgipRequest.getContextSubmitQueue(getContext());
				if (submits.size() > 0) {
					RuntimeException tex = new IllegalArgumentException(String.format("%d unfinished post Submit", submits.size()));
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
		

	public boolean isAutoReSubmit() {
		return retrySubmit;
	}

	public SgipContextSPCHandler() {
		this.handler_type = 1;
	}

	public void setMaxRequestIdleTime(int maxRequestIdleTime) {
		this.maxRequestIdleTime = maxRequestIdleTime;
	}

	@Override
	protected void handleContextInitialized(Context ctx) {
		super.handleContextInitialized(ctx);
		if (_delayContextSubmits != null) {
			SgipRequest.pushSubmits(getContext(), _delayContextSubmits);
		}
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
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

	/**
	 * 提交发送消息
	 * 
	 * @param submits
	 */
	public void postSubmit(List<Submit> submits, int from, int length) {
		if (isStarted()) {
			SgipRequest.pushSubmits(getContext(), submits, from, length);
		} else {
			_delayContextSubmits = submits.subList(from, from + length);
		}
	}

	public void setMaxOnceSubmits(int maxOnceSubmits) {
		this.maxOnceSubmits = maxOnceSubmits;
	}

	public int getMaxOnceSubmits() {
		return maxOnceSubmits;
	}

	@Override
	protected void handleTimeout(Request request, Response response) throws IOException {
		SgipRequest smgp_request = getSgipRequest(request, response);
		SgipResponse smgp_response = getSgipResponse(request, response);
		if (smgp_request.isBinded()) {
			long request_idle_time = System.currentTimeMillis() - smgp_request.getRequestTimestamp();
			if (request_idle_time > maxRequestIdleTime) {
				if (smgp_request.isSubmiting()) {
					int submitted_result_count = smgp_request.getSubmittedResultCount();
					int submitted_count = smgp_request.getSubmittedCount();
					smgp_request.pushSubmits(smgp_request.getSubmitteds(), submitted_result_count, submitted_count
							- submitted_result_count);
					smgp_request.cleanSubmitteds();
				}
				super.handleTimeout(smgp_request, smgp_response);
			}
		} else {
			super.handleTimeout(smgp_request, smgp_response);
		}
	}

	protected void doPostSubmit(SgipRequest request, SgipResponse response) throws IOException {
		int node_id = getNodeId();
		String enterprise_id = Integer.toString(this.enterpriseId);
		String sp_numer = Integer.toString(this.spNumber);
		Integer submitted = 0;
		List<Submit> takedPostSubmits = request.takeSubmits(getMaxOnceSubmits());

		for (Submit sb : takedPostSubmits) {
			sb.sp_number = sp_numer;
			sb.node_id = node_id;
			sb.corporation_id = enterprise_id;
			sb.timestamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
			sb.sequence = request.generateSequence();
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
				request.pushSubmits(takedPostSubmits, 0, takedPostSubmits.size());
				request.cleanSubmitteds();
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

	protected void doSubmitResult(SgipRequest request, SgipResponse response) throws IOException {
		Log.debug(request.getDataPacket().toString());
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
				do_unbind = isAutoReSubmit() ? false : true;
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
			UnBind unbind = new UnBind();
			unbind.node_id = getNodeId();
			unbind.timestamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
			unbind.sequence = request.generateSequence();
			response.writeDataPacket(unbind);
			response.finalBuffer();
		}
	}

	@Override
	protected void doUnBindResponse(SgipRequest request, SgipResponse response) throws IOException {
		super.doUnBindResponse(request, response);
	}

	@Override
	protected void doBindResponse(SgipRequest request, SgipResponse response) throws IOException {
		BindResponse res = (BindResponse) request.getDataPacket();
		if (res.result == BindResults.SUCCESS) {
			request.setBinded(true);
			doPostSubmit(request, response);
		} else {
			throw new BindException("bind error, please check user or password or  validate your ip address");
		}
	}

	@Override
	protected void doSubmitResponse(SgipRequest request, SgipResponse response) throws IOException {
		super.doSubmitResponse(request, response);
		doSubmitResult(request, response);
	}

	@Override
	protected void doBindRequest(SgipRequest request, SgipResponse response) throws IOException {
		Bind bind = new Bind();
		bind.user = account;
		bind.pwd = password;
		bind.type = BindTypes.SP_TO_SMG;

		bind.node_id = getNodeId();
		bind.timestamp = CalendarUtils.getTimestampInYearDuring(System.currentTimeMillis());
		bind.sequence = request.generateSequence();
		response.flushDataPacket(bind);
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

}
