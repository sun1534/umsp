package com.partsoft.umsp.smgp;

import java.io.IOException;

import com.partsoft.umsp.packet.PacketException;
import com.partsoft.umsp.smgp.Constants.LoginModes;

public class SmgpContextSPCHandler extends SmgpContextSPSHandler {

	public SmgpContextSPCHandler() {
		super.setLoginMode(LoginModes.SEND);
	}
	
	@Override
	public void setLoginMode(int loginMode) {
		throw new IllegalStateException("not set login mode");
	}

	@Override
	protected void doDeliver(SmgpRequest request, SmgpResponse response) throws IOException {
		throw new PacketException("not support deliver");
	}
	
	
}
