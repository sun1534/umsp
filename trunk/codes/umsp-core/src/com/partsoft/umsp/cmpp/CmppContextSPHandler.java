package com.partsoft.umsp.cmpp;

import java.io.IOException;

public class CmppContextSPHandler extends CmppContextHandler {

	protected String account;

	protected String password;

	protected int spNumber;

	protected int enterpriseId;

	@Override
	protected void doBind(CmppRequest request, CmppResponse response) throws IOException {
		super.doBind(request, response);

		Connect login = (Connect) request.getDataPacket();
		ConnectResponse bind_resp = new ConnectResponse();
		bind_resp.status = 0;

		response.writeDataPacket(login);
		if (bind_resp.status == 0) {
			bind_resp.authenticationToken = CmppUtils.generateServerToken(bind_resp.status, login.authenticationToken, password);
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

	public void setSpNumber(int spNumber) {
		this.spNumber = spNumber;
	}

}
