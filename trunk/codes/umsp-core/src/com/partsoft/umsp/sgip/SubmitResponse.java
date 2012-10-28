package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class SubmitResponse extends ResponsePacket {
	
	public SubmitResponse() {
		super(Commands.SUBMIT_RESPONSE);
	}

	@Override
	public SubmitResponse clone() {
		return (SubmitResponse) super.clone();
	}

}
