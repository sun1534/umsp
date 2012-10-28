package com.partsoft.umsp.sgip;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.partsoft.umsp.Handler;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.handler.AbstractContextHandler;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.packet.PacketInputStream;
import com.partsoft.umsp.sgip.Constants.Commands;
import com.partsoft.umsp.sgip.Constants.HandlerTypes;
import com.partsoft.utils.Assert;

public class SgipContextHandler extends AbstractContextHandler implements Handler {

	static final String ARG_BINDED = "sgip.user.binded";

	static final String ARG_SGIP_REQUEST = "sgip.request";

	static final String ARG_SGIP_RESPONSE = "sgip.response";

	protected static final Map<Integer, SgipDataPacket> sgip_packet_maps = new HashMap<Integer, SgipDataPacket>(10);

	static {
		addSGIPCommands(new Bind());
		addSGIPCommands(new BindResponse());

		addSGIPCommands(new Submit());
		addSGIPCommands(new SubmitResponse());

		addSGIPCommands(new Deliver());
		addSGIPCommands(new DeliverResponse());

		addSGIPCommands(new Report());
		addSGIPCommands(new ReportResponse());

		addSGIPCommands(new UnBind());
		addSGIPCommands(new UnBindResponse());
	}

	private Map<Integer, SgipDataPacket> context_sgip_packet_maps;

	protected int handler_type = 2;

	protected static void addSGIPCommands(SgipDataPacket packet) {
		sgip_packet_maps.put(packet.getCommand(), packet);
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		context_sgip_packet_maps = new HashMap<Integer, SgipDataPacket>(sgip_packet_maps);
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		context_sgip_packet_maps = null;
	}

	public SgipContextHandler() {

	}

	protected void doBind(SgipRequest request, SgipResponse response) throws IOException {
		if (request.isBinded()) {
			Log.warn(String.format("duplicate error bind request packet, packet is:\n%s", request.getDataPacket()
					.toString()));
			response.finalBuffer();
		}

		if (handler_type == HandlerTypes.SP_CLIENT) {
			Log.warn(String.format("client endpoint receive bind request packet? packet is:\n%s", request
					.getDataPacket().toString()));
		}
	}

	protected void doBindResponse(SgipRequest request, SgipResponse response) throws IOException {

	}

	protected void doUnBind(SgipRequest request, SgipResponse response) throws IOException {
		response.finalBuffer();
	}

	protected void doUnBindResponse(SgipRequest request, SgipResponse response) throws IOException {
		response.finalBuffer();
	}

	protected void doReport(SgipRequest request, SgipResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

	}

	protected void doReportResponse(SgipRequest request, SgipResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
	}

	protected void doDeliver(SgipRequest request, SgipResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
	}

	protected void doDeliverResponse(SgipRequest request, SgipResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
	}

	protected void doSubmit(SgipRequest request, SgipResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

	}

	protected void doSubmitResponse(SgipRequest request, SgipResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

	}

	protected void doBindRequest(SgipRequest request, SgipResponse response) throws IOException {
		response.flushBuffer();
	}

	private void doRequestStart(SgipRequest request, SgipResponse response) throws IOException {
		doBindRequest(request, response);
	}

	protected SgipRequest getSgipRequest(Request request, Response response) {
		SgipRequest new_request = null;
		if (!(request instanceof SgipRequest)) {
			new_request = (SgipRequest) request.getAttribute(ARG_SGIP_REQUEST);
			if (new_request == null) {
				new_request = new SgipRequest(request);
				request.setAttribute(ARG_SGIP_REQUEST, new_request);
			} else {
				new_request.setWrapper(request);
			}
		} else {
			new_request = (SgipRequest) request;
		}
		return new_request;
	}

	protected SgipResponse getSgipResponse(Request request, Response response) {
		SgipResponse new_response = null;
		if (!(response instanceof SgipResponse)) {
			new_response = (SgipResponse) request.getAttribute(ARG_SGIP_RESPONSE);
			if (new_response == null) {
				new_response = new SgipResponse(response);
				request.setAttribute(ARG_SGIP_RESPONSE, new_response);
			} else {
				new_response.setWrapper(response);
			}
		} else {
			new_response = (SgipResponse) response;
		}
		return new_response;
	}

	@Override
	protected void handleRequest(Request request, Response response) throws IOException {
		try {
			SgipRequest rel_req = getSgipRequest(request, response);
			SgipResponse rel_res = getSgipResponse(request, response);

			if (request.getRequests() == 1) {
				// 请求开始
				doRequestStart(rel_req, rel_res);
				return;
			}
			SgipDataPacket packet = readPacketObject(rel_req.getPacketInputStream());
			rel_req.setDataPacket(packet);
			switch (packet.command) {
			case Commands.BIND:
				doBind(rel_req, rel_res);
				break;
			case Commands.BIND_RESPONSE:
				doBindResponse(rel_req, rel_res);
				break;
			case Commands.UNBIND:
				doUnBind(rel_req, rel_res);
				break;
			case Commands.UNBIND_RESPONSE:
				doUnBindResponse(rel_req, rel_res);
				break;
			case Commands.DELIVER:
				doDeliver(rel_req, rel_res);
				break;
			case Commands.DELIVER_RESPONSE:
				doDeliverResponse(rel_req, rel_res);
				break;
			case Commands.SUBMIT:
				doSubmit(rel_req, rel_res);
				break;
			case Commands.SUBMIT_RESPONSE:
				doSubmitResponse(rel_req, rel_res);
				break;
			case Commands.REPORT:
				doReport(rel_req, rel_res);
				break;
			case Commands.REPORT_RESPONSE:
				doReportResponse(rel_req, rel_res);
				break;
			default:
				rel_res.finalBuffer();
				break;
			}
		} finally {
			response.flushBuffer();
		}
	}

	public SgipDataPacket readPacketObject(PacketInputStream in) throws PacketException {
		if (in == null)
			throw new PacketException("null in");
		SgipDataPacket result = null;
		try {
			in.mark(Buffer.INT_SIZE); 
			int command = in.readInt();
			in.reset();
			SgipDataPacket packet = context_sgip_packet_maps.get(command);
			if (packet != null) {
				packet = packet.clone();
				packet.readExternal(in);
				result = packet;
			} else
				throw new IOException(String.format("not found command: %d", command));
		} catch (IOException e) {
			Log.error(e.getMessage(), e);
			throw new PacketException(e);
		} catch (ClassNotFoundException e) {
			throw new PacketException(e);
		}
		return result;
	}

}
