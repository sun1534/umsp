package com.partsoft.umsp.sgip;

import java.io.IOException;

import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.utils.CompareUtils;
import com.partsoft.utils.StringUtils;

public abstract class AbstractSgipSPReceiveHandler extends AbstractSgipContextSPHandler {
	
	public AbstractSgipSPReceiveHandler() {
		
	}
	
	@Override
	protected void doBindRequest(Request request, Response response)
			throws IOException {
		// 用于接收处理器（作为服务方）， 不需要绑定请求。
	}
	
	@Override
	protected void doBind(Request request, Response response) throws IOException {
		Bind bind = (Bind) SgipUtils.extractRequestPacket(request);
		BindResponse bind_resp = new BindResponse();
		SgipUtils.copySerialNumber(bind_resp, bind);

		bind_resp.result = BindResults.SUCCESS;
		if (limitClientIp && StringUtils.hasText(smgHost)
				&& !CompareUtils.nullSafeEquals(smgHost, request.getRemoteHost())) {
			bind_resp.result = BindResults.IPERROR;
		}
		SgipUtils.renderDataPacket(request, response, bind_resp);
		if (bind_resp.result == BindResults.SUCCESS) {
			SgipUtils.setupRequestBinded(request, true);
			response.flushBuffer();
		} else {
			response.finalBuffer();
		}
	}
	
	@Override
	protected void doDeliver(Request request, Response response) throws IOException {
		super.doDeliver(request, response);
		Deliver deliver_packet = (Deliver) SgipUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}
		byte result = 9;
		try {
			doReceivedMessage(deliver_packet);
			result = 0;
		} catch (Throwable e) {
			Log.error(e.getMessage(), e);
		}
		DeliverResponse resp = new DeliverResponse();
		SgipUtils.copySerialNumber(resp, deliver_packet);
		resp.result = result;
		SgipUtils.renderDataPacket(request, response, resp);
		response.flushBuffer();
	}
	
	protected abstract void doReceivedMessage(Deliver deliver);
	
	protected abstract void doReceivedReport(Report report);
	
	@Override
	protected void doReport(Request request, Response response) throws IOException {
		super.doReport(request, response);
		Report report_packet = (Report) SgipUtils.extractRequestPacket(request);
		if (Log.isDebugEnabled()) {
			Log.debug(report_packet.toString());
		}
		byte result = 9;
		try {
			doReceivedReport(report_packet);
		} catch (Throwable e) {
			Log.error(e.getMessage(), e);
		}
		ReportResponse resp = new ReportResponse();
		SgipUtils.copySerialNumber(resp, report_packet);
		resp.result = result;
		SgipUtils.renderDataPacket(request, response, resp);
		response.flushBuffer();
	}
	
	@Override
	protected void doSubmit(Request request, Response response) throws IOException {
		throw new PacketException("not support");
	}
	
}
