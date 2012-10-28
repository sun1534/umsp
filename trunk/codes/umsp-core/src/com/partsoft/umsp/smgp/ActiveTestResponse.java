package com.partsoft.umsp.smgp;

import com.partsoft.umsp.smgp.Constants.RequestIDs;

public class ActiveTestResponse extends SmgpDataPacket {

	public ActiveTestResponse() {
		super(RequestIDs.active_test_resp);
	}
	
	@Override
	public ActiveTestResponse clone() {
		return (ActiveTestResponse) super.clone();
	}

}
