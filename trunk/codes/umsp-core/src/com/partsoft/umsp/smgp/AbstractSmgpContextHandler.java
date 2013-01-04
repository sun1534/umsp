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

public abstract class AbstractSmgpContextHandler extends AbstractContextHandler implements Handler {

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

	public AbstractSmgpContextHandler() {

	}

	protected void doBind(Request request, Response response) throws IOException {
		if (SmgpUtils.testRequestBinded(request)) {
			Log.warn(String.format("duplicate error bind request packet, packet is:\n%s", SmgpUtils
					.extractRequestPacket(request).toString()));
			response.finalBuffer();
		}
	}

	protected void doActiveTest(Request request, Response response) throws IOException {
		Assert.isTrue(SmgpUtils.testRequestBinded(request));
		ActiveTestResponse test_response = ((ActiveTestResponse) smgp_packet_maps.get(RequestIDs.active_test_resp))
				.clone();
		test_response.sequenceId = SmgpUtils.generateRequestSequence(request);
		SmgpUtils.renderDataPacket(request, response, test_response);
		response.flushBuffer();
	}

	protected void doActiveTestRequest(Request request, Response response) throws IOException {
		ActiveTest test = ((ActiveTest) smgp_packet_maps.get(RequestIDs.active_test)).clone();
		test.sequenceId = SmgpUtils.generateRequestSequence(request);
		SmgpUtils.renderDataPacket(request, response, test);
		response.flushBuffer();
	}

	protected void doActiveTestResponse(Request request, Response response) throws IOException {
		Assert.isTrue(SmgpUtils.testRequestBinded(request));
	}

	protected void doBindResponse(Request request, Response response) throws IOException {

	}

	protected void doUnBind(Request request, Response response) throws IOException {
		ExitResponse exit_res = (ExitResponse) smgp_packet_maps.get(RequestIDs.exit_resp).clone();
		exit_res.sequenceId = SmgpUtils.generateRequestSequence(request);
		SmgpUtils.renderDataPacket(request, response, exit_res);
		response.finalBuffer();
	}

	protected void doUnBindResponse(Request request, Response response) throws IOException {
		response.finalBuffer();
	}

	protected void doDeliver(Request request, Response response) throws IOException {
		Assert.isTrue(SmgpUtils.testRequestBinded(request));
	}

	protected void doDeliverResponse(Request request, Response response) throws IOException {
		Assert.isTrue(SmgpUtils.testRequestBinded(request));
	}

	protected void doSubmit(Request request, Response response) throws IOException {
		Assert.isTrue(SmgpUtils.testRequestBinded(request));
	}

	protected void doSubmitResponse(Request request, Response response) throws IOException {
		Assert.isTrue(SmgpUtils.testRequestBinded(request));
	}

	protected abstract void doBindRequest(Request request, Response response) throws IOException;

	private void doRequestStart(Request request, Response response) throws IOException {
		doBindRequest(request, response);
	}

	@Override
	protected void handleConnect(Request request, Response response) throws IOException {
		doRequestStart(request, response);
	}
	
	@Override
	protected void handleDisConnect(Request request, Response response) {
		SmgpUtils.cleanRequestAttributes(request);
	}

	@Override
	protected void handleRequest(Request request, Response response) throws IOException {
		try {
			SmgpUtils.cleanRequestActiveTesting(request);
			SmgpDataPacket packet = readPacketObject(SmgpUtils.extractRequestPacketStream(request));
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("packet: %s", packet.toString()));
			}
			SmgpUtils.setupRequestPacket(request, packet);
			switch (packet.requestId) {
			case RequestIDs.login:
				doBind(request, response);
				break;
			case RequestIDs.login_resp:
				doBindResponse(request, response);
				break;
			case RequestIDs.exit:
				doUnBind(request, response);
				break;
			case RequestIDs.exit_resp:
				doUnBindResponse(request, response);
				break;
			case RequestIDs.deliver:
				doDeliver(request, response);
				break;
			case RequestIDs.deliver_resp:
				doDeliverResponse(request, response);
				break;
			case RequestIDs.submit:
				doSubmit(request, response);
				break;
			case RequestIDs.submit_resp:
				doSubmitResponse(request, response);
				break;
			case RequestIDs.active_test:
				doActiveTest(request, response);
				break;
			case RequestIDs.active_test_resp:
				doActiveTestResponse(request, response);
				break;
			default:
				response.finalBuffer();
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
		}
		return result;
	}

}
