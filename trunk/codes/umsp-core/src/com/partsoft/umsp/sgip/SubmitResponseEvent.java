package com.partsoft.umsp.sgip;

import java.util.EventObject;

public class SubmitResponseEvent extends EventObject {

	private static final long serialVersionUID = 4797267399853295565L;
	
	private Submit submit;
	
	public SubmitResponseEvent(Submit submit, SubmitResponse source) {
		super(source);
		this.submit = submit;
	}
	
	public SubmitResponse getSubmitResponse() {
		return (SubmitResponse) super.getSource();
	}
	
	public Submit getSubmit() {
		return submit;
	}

}
