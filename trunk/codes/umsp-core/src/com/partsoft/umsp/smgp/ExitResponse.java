package com.partsoft.umsp.smgp;

import com.partsoft.umsp.smgp.Constants.RequestIDs;

public class ExitResponse extends SmgpDataPacket {

	public ExitResponse() {
		super(RequestIDs.exit_resp);
	}
	
	@Override
	public ExitResponse clone() {
		return (ExitResponse) super.clone();
	}

}
