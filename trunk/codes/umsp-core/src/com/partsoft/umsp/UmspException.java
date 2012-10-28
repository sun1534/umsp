package com.partsoft.umsp;

@SuppressWarnings("serial")
public class UmspException extends Exception {

	private String errorCode;

	public UmspException(String reason) {
		super(reason);
	}

	public UmspException(String reason, String errorCode) {
		super(reason);
		this.errorCode = errorCode;
	}
	
	public UmspException(String reason, Throwable e) {
		super(reason, e);
	}

	public void finalize() {
		errorCode = null;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public Throwable getLinkedException() {
		return getCause();
	}
}