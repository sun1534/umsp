package com.partsoft.umsp.sgip;

import java.io.IOException;

import com.partsoft.umsp.sgip.Constants.BindResults;
import com.partsoft.utils.CompareUtils;
import com.partsoft.utils.StringUtils;

public class SgipContextSPHandler extends SgipContextHandler {

	public static final String ARG_SEQUENCE = "sgip.context.sequence";

	protected String account;

	protected String password;

	protected String smgHost;

	protected int spNumber;

	protected int enterpriseId;

	protected int areaId;

	private int nodeId = 0;

	protected boolean limitClientIp;

	public void setLimitClientIp(boolean limitClientIp) {
		this.limitClientIp = limitClientIp;
	}

	@Override
	protected void doBind(SgipRequest request, SgipResponse response) throws IOException {
		super.doBind(request, response);

		Bind bind = (Bind) request.getDataPacket();
		BindResponse bind_resp = new BindResponse();
		SgipUtils.copySerialNumber(bind_resp, bind);

		bind_resp.result = BindResults.SUCCESS;

		// if (!(CompareUtils.nullSafeEquals(bind.user, account) &&
		// CompareUtils.nullSafeEquals(bind.pwd, password))) {
		// bind_resp.result = BindResults.ERROR;
		// } else
		if (limitClientIp && StringUtils.hasText(smgHost)
				&& !CompareUtils.nullSafeEquals(smgHost, request.getRemoteHost())) {
			bind_resp.result = BindResults.IPERROR;
		}
		response.writeDataPacket(bind_resp);
		if (bind_resp.result == BindResults.SUCCESS) {
			request.setBinded(true);
			response.flushBuffer();
		} else {
			response.finalBuffer();
		}
	}

	public int getNodeId() {
		if (nodeId == 0) {
			int aid = areaId;
			nodeId = (int) (3000000000L + aid * 100000 + enterpriseId);
		}
		return nodeId;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setSmgHost(String smgHost) {
		this.smgHost = smgHost;
	}

	public void setSpNumber(int spNumber) {
		this.spNumber = spNumber;
	}

	public void setEnterpriseId(int enterpriseId) {
		this.enterpriseId = enterpriseId;
		this.nodeId = 0;
	}

	public void setAreaId(int areaId) {
		this.areaId = areaId;
		this.nodeId = 0;
	}

}
