package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class BindResponse extends ResponsePacket {

	public BindResponse() {
		super(Commands.BIND_RESPONSE);
	}

}
