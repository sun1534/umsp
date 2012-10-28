package com.partsoft.umsp.smgp;

import java.io.IOException;

import com.partsoft.umsp.smgp.Constants.StatusCodes;

public class SmgpContextSPHandler extends SmgpContextHandler {

	protected String account;

	protected String password;

	protected int spNumber;

	protected int enterpriseId;

	@Override
	protected void doBind(SmgpRequest request, SmgpResponse response) throws IOException {
		super.doBind(request, response);

		Login login = (Login) request.getDataPacket();
		LoginResponse bind_resp = new LoginResponse();
		bind_resp.Status = StatusCodes.ERR_SUCCESS;

		response.writeDataPacket(login);
		if (bind_resp.Status == StatusCodes.ERR_SUCCESS) {
			bind_resp.AuthenticatorServer = SmgpUtils.generateServerToken(bind_resp.Status, login.AuthenticatorClient, password);
			response.writeDataPacket(bind_resp);
			request.setBinded(true);
			response.flushBuffer();
		} else {
			response.finalBuffer();
		}
	}


	public void setAccount(String account) {
		this.account = account;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setEnterpriseId(int enterpriseId) {
		this.enterpriseId = enterpriseId;
	}

	public int getSpNumber() {
		return spNumber;
	}

	public void setSpNumber(int spNumber) {
		this.spNumber = spNumber;
	}
	

}
