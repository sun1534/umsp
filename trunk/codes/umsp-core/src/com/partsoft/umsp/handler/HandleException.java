package com.partsoft.umsp.handler;

public class HandleException extends RuntimeException {

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

	public HandleException(String message, Object transmitObject) {
		super(message);
		this.handleObject = transmitObject;
	}

}
