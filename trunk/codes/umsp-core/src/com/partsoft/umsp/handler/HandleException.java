package com.partsoft.umsp.handler;

import com.partsoft.umsp.UmspException;

public class HandleException extends UmspException {

	private static final long serialVersionUID = -1870326620030346018L;

	private Object handleObject;

	public Object getHandleObject() {
		return handleObject;
	}

	public HandleException(String message, Throwable cause, Object transmitObject) {
		super(message, cause);
		this.handleObject = transmitObject;
	}

	public HandleException(Throwable cause, Object transmitObject) {
		super(cause);
		this.handleObject = transmitObject;
	}

	public HandleException(Object transmitObject) {
		super();
		this.handleObject = transmitObject;
	}
	
	@Override
	public void finalize() {
		super.finalize();
		this.handleObject = null;
	}

	public HandleException(String message, Object transmitObject) {
		super(message);
		this.handleObject = transmitObject;
	}

}
