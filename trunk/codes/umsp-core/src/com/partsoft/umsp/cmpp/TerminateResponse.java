package com.partsoft.umsp.cmpp;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class TerminateResponse extends CmppDataPacket {

	public TerminateResponse() {
		super(Commands.CMPP_TERMINATE_RESP);
	}
	
	@Override
	public TerminateResponse clone() {
		return (TerminateResponse) super.clone();
	}
	
}
