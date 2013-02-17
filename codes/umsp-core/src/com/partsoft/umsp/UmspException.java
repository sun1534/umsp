package com.partsoft.umsp;

public class UmspException extends RuntimeException {

	private static final long serialVersionUID = -6879583987345986122L;
	
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
	
	public UmspException() {
		super();
		this.errorCode = "UNKNOWN";
	}

	public UmspException(Throwable cause) {
		super(cause);
		this.errorCode = "UNKNOWN";
	}

	public void finalize() {
		try {
			super.finalize();
		} catch (Throwable e) {
			
		}
		errorCode = null;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public Throwable getLinkedException() {
		return getCause();
	}
}