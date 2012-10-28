package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class DeliverResponse extends ResponsePacket {
	
	public DeliverResponse() {
		super(Commands.DELIVER_RESPONSE);
	}

	@Override
	public DeliverResponse clone() {
		return (DeliverResponse) super.clone();
	}

}
