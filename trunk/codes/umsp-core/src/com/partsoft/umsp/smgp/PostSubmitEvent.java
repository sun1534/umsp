package com.partsoft.umsp.smgp;

import java.util.EventObject;

public class PostSubmitEvent extends EventObject {

	private static final long serialVersionUID = 4797267399853295565L;
	
	public PostSubmitEvent(Submit source) {
		super(source);
	}
	
	public Submit getSubmit() {
		return (Submit) super.getSource();
	}

}
