package com.partsoft.umsp.cmpp;

import java.io.IOException;
import com.partsoft.umsp.packet.PacketException;

public class CmppContextSPCHandler extends CmppContextSPSHandler {

	@Override
	protected void doDeliver(CmppRequest request, CmppResponse response) throws IOException {
		throw new PacketException("not support deliver");
	}
	
}
