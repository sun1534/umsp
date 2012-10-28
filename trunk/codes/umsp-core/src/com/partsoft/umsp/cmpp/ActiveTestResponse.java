package com.partsoft.umsp.cmpp;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class ActiveTestResponse extends CmppDataPacket {

	protected ActiveTestResponse() {
		super(Commands.CMPP_ACTIVE_TEST_RESP);
	}

	@Override
	public ActiveTestResponse clone() {
		return (ActiveTestResponse) super.clone();
	}
	
}
