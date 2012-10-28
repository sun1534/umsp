package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class ReportResponse extends ResponsePacket {
	
	public ReportResponse() {
		super(Commands.REPORT_RESPONSE);
	}

	@Override
	public ReportResponse clone() {
		return (ReportResponse) super.clone();
	}

}
