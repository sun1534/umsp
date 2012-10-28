package com.partsoft.umsp.cmpp;

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
import com.partsoft.umsp.cmpp.Constants.Commands;
import com.partsoft.utils.Assert;

public abstract class CmppContextHandler extends AbstractContextHandler implements Handler {

	protected static final Map<Integer, CmppDataPacket> cmpp_packet_maps = new HashMap<Integer, CmppDataPacket>(10);

	static {
		addCmppCommands(new Connect());
		addCmppCommands(new ConnectResponse());

		addCmppCommands(new Submit());
		addCmppCommands(new SubmitResponse());

		addCmppCommands(new Deliver());
		addCmppCommands(new DeliverResponse());

		addCmppCommands(new ActiveTest());
		addCmppCommands(new ActiveTestResponse());

		addCmppCommands(new Terminate());
		addCmppCommands(new TerminateResponse());
	}

	private Map<Integer, CmppDataPacket> context_smgp_packet_maps;

	protected static void addCmppCommands(CmppDataPacket packet) {
		cmpp_packet_maps.put(packet.getCommandId(), packet);
	}
	
	protected int protocolVersion = Constants.VERSION2;

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		context_smgp_packet_maps = new HashMap<Integer, CmppDataPacket>(cmpp_packet_maps);
		//设置版本号
		for (CmppDataPacket packet : context_smgp_packet_maps.values()) {
			packet.protocolVersion = this.protocolVersion;
		}
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		context_smgp_packet_maps = null;
	}

	public CmppContextHandler() {

	}

	protected void doBind(CmppRequest request, CmppResponse response) throws IOException {
		if (request.isBinded()) {
			Log.warn(String.format("duplicate error bind request packet, packet is:\n%s", request.getDataPacket()
					.toString()));
			response.finalBuffer();
		}
	}

	protected void doActiveTest(CmppRequest request, CmppResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

		ActiveTestResponse test_response = ((ActiveTestResponse) cmpp_packet_maps.get(Commands.CMPP_ACTIVE_TEST_RESP))
				.clone();
		test_response.sequenceId = request.generateSequence();
		response.flushDataPacket(test_response);
	}

	protected void doActiveTestRequest(CmppRequest request, CmppResponse response) throws IOException {
		ActiveTest test = ((ActiveTest) cmpp_packet_maps.get(Commands.CMPP_ACTIVE_TEST)).clone();
		test.sequenceId = request.generateSequence();
		response.flushDataPacket(test);
	}

	protected void doActiveTestResponse(CmppRequest request, CmppResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
		ActiveTestResponse test_response = (ActiveTestResponse) request.getDataPacket();
		Log.debug(test_response.toString());
	}

	protected void doBindResponse(CmppRequest request, CmppResponse response) throws IOException {

	}

	protected void doUnBind(CmppRequest request, CmppResponse response) throws IOException {
		TerminateResponse exit_res = (TerminateResponse) cmpp_packet_maps.get(Commands.CMPP_TERMINATE_RESP).clone();
		exit_res.sequenceId = request.generateSequence();
		response.writeDataPacket(exit_res);
		response.finalBuffer();
	}

	protected void doUnBindResponse(CmppRequest request, CmppResponse response) throws IOException {
		response.finalBuffer();
	}

	protected void doDeliver(CmppRequest request, CmppResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
	}

	protected void doDeliverResponse(CmppRequest request, CmppResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
	}

	protected void doSubmit(CmppRequest request, CmppResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

	}

	protected void doSubmitResponse(CmppRequest request, CmppResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

	}

	protected void doBindRequest(CmppRequest request, CmppResponse response) throws IOException {
		response.flushBuffer();
	}

	private void doRequestStart(CmppRequest request, CmppResponse response) throws IOException {
		doBindRequest(request, response);
	}

	protected CmppRequest getCmppRequest(Request request, Response response) {
		CmppRequest new_request = null;
		if (!(request instanceof CmppRequest)) {
			new_request = (CmppRequest) request.getAttribute(CmppRequest.ARG_REQUEST);
			if (new_request == null) {
				new_request = new CmppRequest(request);
				request.setAttribute(CmppRequest.ARG_REQUEST, new_request);
			} else {
				new_request.setWrapper(request);
			}
		} else {
			new_request = (CmppRequest) request;
		}
		return new_request;
	}

	protected CmppResponse getCmppResponse(Request request, Response response) {
		CmppResponse new_response = null;
		if (!(response instanceof CmppResponse)) {
			new_response = (CmppResponse) request.getAttribute(CmppResponse.ARG_RESPONSE);
			if (new_response == null) {
				new_response = new CmppResponse(response);
				request.setAttribute(CmppResponse.ARG_RESPONSE, new_response);
			} else {
				new_response.setWrapper(response);
			}
		} else {
			new_response = (CmppResponse) response;
		}
		return new_response;
	}

	@Override
	protected void handleRequest(Request request, Response response) throws IOException {
		try {
			CmppRequest rel_req = getCmppRequest(request, response);
			CmppResponse rel_res = getCmppResponse(request, response);
			rel_req.cleanActiveTesting();

			if (request.getRequests() == 1) {
				// 请求开始
				doRequestStart(rel_req, rel_res);
				return;
			}

			CmppDataPacket packet = readPacketObject(rel_req.getPacketInputStream());
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("packet: %s", packet.toString()));
			}
			rel_req.setDataPacket(packet);
			switch (packet.getCommandId()) {
			case Commands.CMPP_CONNECT:
				doBind(rel_req, rel_res);
				break;
			case Commands.CMPP_CONNECT_RESP:
				doBindResponse(rel_req, rel_res);
				break;
			case Commands.CMPP_TERMINATE:
				doUnBind(rel_req, rel_res);
				break;
			case Commands.CMPP_TERMINATE_RESP:
				doUnBindResponse(rel_req, rel_res);
				break;
			case Commands.CMPP_DELIVER:
				doDeliver(rel_req, rel_res);
				break;
			case Commands.CMPP_DELIVER_RESP:
				doDeliverResponse(rel_req, rel_res);
				break;
			case Commands.CMPP_SUBMIT:
				doSubmit(rel_req, rel_res);
				break;
			case Commands.CMPP_SUBMIT_RESP:
				doSubmitResponse(rel_req, rel_res);
				break;
			case Commands.CMPP_ACTIVE_TEST:
				doActiveTest(rel_req, rel_res);
				break;
			case Commands.CMPP_ACTIVE_TEST_RESP:
				doActiveTestResponse(rel_req, rel_res);
				break;
			default:
				rel_res.finalBuffer();
				break;
			}
		} finally {
			response.flushBuffer();
		}
	}

	public CmppDataPacket readPacketObject(PacketInputStream in) throws PacketException {
		CmppDataPacket result = null;
		try {
			in.mark(Buffer.INT_SIZE);
			int command = in.readInt();
			in.reset();
			CmppDataPacket packet = context_smgp_packet_maps.get(command);
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
	
	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

}
