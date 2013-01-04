package com.partsoft.umsp.sgip;

public abstract class AbstractSgipContextSPHandler extends AbstractSgipContextHandler {

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
