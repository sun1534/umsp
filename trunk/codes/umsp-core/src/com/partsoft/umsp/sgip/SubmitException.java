package com.partsoft.umsp.sgip;

public class SubmitException extends SgipException {

	private static final long serialVersionUID = 4531875680564123334L;

	public SubmitException(int size, int position, int offset) {
		super(size, position, offset);
	}

	public SubmitException(int size, int offset) {
		super(size, offset);
	}

	public SubmitException(String reason, Throwable e) {
		super(reason, e);
	}

	public SubmitException(String reason) {
		super(reason);
	}

	public SubmitException(Throwable e) {
		super(e);
	}
	
}
