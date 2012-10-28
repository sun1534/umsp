package com.partsoft.umsp.cmpp;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class ActiveTest extends CmppDataPacket {

	protected ActiveTest() {
		super(Commands.CMPP_ACTIVE_TEST);
	}

	@Override
	public ActiveTest clone() {
		return (ActiveTest) super.clone();
	}
	
}
