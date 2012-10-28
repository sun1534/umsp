package com.partsoft.umsp.sgip;

import java.io.IOException;

import com.partsoft.umsp.log.Log;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.sgip.Constants.HandlerTypes;

public class SgipContextSPSHandler extends SgipContextSPHandler {
	
	public SgipContextSPSHandler() {
		this.handler_type = HandlerTypes.SP_SERVER;
	}
	
	@Override
	protected void doDeliver(SgipRequest request, SgipResponse response) throws IOException {
		super.doDeliver(request, response);
		Deliver deliver_packet = (Deliver) request.getDataPacket();
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}
	}
	
	@Override
	protected void doReport(SgipRequest request, SgipResponse response) throws IOException {
		super.doReport(request, response);
		Report deliver_packet = (Report) request.getDataPacket();
		if (Log.isDebugEnabled()) {
			Log.debug(deliver_packet.toString());
		}
	}
	
	@Override
	protected void doSubmit(SgipRequest request, SgipResponse response) throws IOException {
		throw new PacketException("not support");
	}
	
}
