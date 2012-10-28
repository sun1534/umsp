package com.partsoft.umsp.smgp;

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
import com.partsoft.umsp.smgp.Constants.RequestIDs;
import com.partsoft.utils.Assert;

public abstract class SmgpContextHandler extends AbstractContextHandler implements Handler {

	protected static final Map<Integer, SmgpDataPacket> smgp_packet_maps = new HashMap<Integer, SmgpDataPacket>(10);

	static {
		addSmgpCommands(new Login());
		addSmgpCommands(new LoginResponse());

		addSmgpCommands(new Submit());
		addSmgpCommands(new SubmitResponse());

		addSmgpCommands(new Deliver());
		addSmgpCommands(new DeliverResponse());

		addSmgpCommands(new ActiveTest());
		addSmgpCommands(new ActiveTestResponse());

		addSmgpCommands(new Exit());
		addSmgpCommands(new ExitResponse());
	}

	private Map<Integer, SmgpDataPacket> context_smgp_packet_maps;

	protected static void addSmgpCommands(SmgpDataPacket packet) {
		smgp_packet_maps.put(packet.getRequestId(), packet);
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		context_smgp_packet_maps = new HashMap<Integer, SmgpDataPacket>(smgp_packet_maps);
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		context_smgp_packet_maps = null;
	}

	public SmgpContextHandler() {

	}

	protected void doBind(SmgpRequest request, SmgpResponse response) throws IOException {
		if (request.isBinded()) {
			Log.warn(String.format("duplicate error bind request packet, packet is:\n%s", request.getDataPacket()
					.toString()));
			response.finalBuffer();
		}
	}

	protected void doActiveTest(SmgpRequest request, SmgpResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

		ActiveTestResponse test_response = ((ActiveTestResponse) smgp_packet_maps.get(RequestIDs.active_test_resp))
				.clone();
		test_response.sequenceId = request.generateSequence();
		response.flushDataPacket(test_response);
	}

	protected void doActiveTestRequest(SmgpRequest request, SmgpResponse response) throws IOException {
		ActiveTest test = ((ActiveTest) smgp_packet_maps.get(RequestIDs.active_test)).clone();
		test.sequenceId = request.generateSequence();
		response.flushDataPacket(test);
	}

	protected void doActiveTestResponse(SmgpRequest request, SmgpResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
		ActiveTestResponse test_response = (ActiveTestResponse) request.getDataPacket();
		Log.debug(test_response.toString());
	}

	protected void doBindResponse(SmgpRequest request, SmgpResponse response) throws IOException {

	}

	protected void doUnBind(SmgpRequest request, SmgpResponse response) throws IOException {
		ExitResponse exit_res = (ExitResponse) smgp_packet_maps.get(RequestIDs.exit_resp).clone();
		exit_res.sequenceId = request.generateSequence();
		response.writeDataPacket(exit_res);
		response.finalBuffer();
	}

	protected void doUnBindResponse(SmgpRequest request, SmgpResponse response) throws IOException {
		response.finalBuffer();
	}

	protected void doDeliver(SmgpRequest request, SmgpResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
	}

	protected void doDeliverResponse(SmgpRequest request, SmgpResponse response) throws IOException {
		Assert.isTrue(request.isBinded());
	}

	protected void doSubmit(SmgpRequest request, SmgpResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

	}

	protected void doSubmitResponse(SmgpRequest request, SmgpResponse response) throws IOException {
		Assert.isTrue(request.isBinded());

	}

	protected void doBindRequest(SmgpRequest request, SmgpResponse response) throws IOException {
		response.flushBuffer();
	}

	private void doRequestStart(SmgpRequest request, SmgpResponse response) throws IOException {
		doBindRequest(request, response);
	}

	protected SmgpRequest getSmgpRequest(Request request, Response response) {
		SmgpRequest new_request = null;
		if (!(request instanceof SmgpRequest)) {
			new_request = (SmgpRequest) request.getAttribute(SmgpRequest.ARG_REQUEST);
			if (new_request == null) {
				new_request = new SmgpRequest(request);
				request.setAttribute(SmgpRequest.ARG_REQUEST, new_request);
			} else {
				new_request.setWrapper(request);
			}
		} else {
			new_request = (SmgpRequest) request;
		}
		return new_request;
	}

	protected SmgpResponse getSmgpResponse(Request request, Response response) {
		SmgpResponse new_response = null;
		if (!(response instanceof SmgpResponse)) {
			new_response = (SmgpResponse) request.getAttribute(SmgpResponse.ARG_RESPONSE);
			if (new_response == null) {
				new_response = new SmgpResponse(response);
				request.setAttribute(SmgpResponse.ARG_RESPONSE, new_response);
			} else {
				new_response.setWrapper(response);
			}
		} else {
			new_response = (SmgpResponse) response;
		}
		return new_response;
	}

	@Override
	protected void handleRequest(Request request, Response response) throws IOException {
		try {
			SmgpRequest rel_req = getSmgpRequest(request, response);
			SmgpResponse rel_res = getSmgpResponse(request, response);
			rel_req.cleanActiveTesting();

			if (request.getRequests() == 1) {
				// 请求开始
				doRequestStart(rel_req, rel_res);
				return;
			}

			SmgpDataPacket packet = readPacketObject(rel_req.getPacketInputStream());
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("packet: %s", packet.toString()));
			}
			rel_req.setDataPacket(packet);
			switch (packet.requestId) {
			case RequestIDs.login:
				doBind(rel_req, rel_res);
				break;
			case RequestIDs.login_resp:
				doBindResponse(rel_req, rel_res);
				break;
			case RequestIDs.exit:
				doUnBind(rel_req, rel_res);
				break;
			case RequestIDs.exit_resp:
				doUnBindResponse(rel_req, rel_res);
				break;
			case RequestIDs.deliver:
				doDeliver(rel_req, rel_res);
				break;
			case RequestIDs.deliver_resp:
				doDeliverResponse(rel_req, rel_res);
				break;
			case RequestIDs.submit:
				doSubmit(rel_req, rel_res);
				break;
			case RequestIDs.submit_resp:
				doSubmitResponse(rel_req, rel_res);
				break;
			case RequestIDs.active_test:
				doActiveTest(rel_req, rel_res);
				break;
			case RequestIDs.active_test_resp:
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

	public SmgpDataPacket readPacketObject(PacketInputStream in) throws PacketException {
		SmgpDataPacket result = null;
		try {
			in.mark(Buffer.INT_SIZE);
			int command = in.readInt();
			in.reset();
			SmgpDataPacket packet = context_smgp_packet_maps.get(command);
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
