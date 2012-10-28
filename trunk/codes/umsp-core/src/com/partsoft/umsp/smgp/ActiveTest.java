package com.partsoft.umsp.smgp;

import com.partsoft.umsp.smgp.Constants.RequestIDs;

public class ActiveTest extends SmgpDataPacket {

	public ActiveTest() {
		super(RequestIDs.active_test);
	}
	
	@Override
	public ActiveTest clone() {
		return (ActiveTest) super.clone();
	}

}
