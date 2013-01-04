package com.partsoft.umsp.smgp;

import java.io.IOException;
import java.util.List;

import com.partsoft.umsp.BatchPool;
import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.packet.PacketException;
import com.partsoft.utils.Assert;

/**
 * 长连接发送通道
 * @author neeker
 *
 */
public class BatchPoolSmgpSPSendHandler extends AbstractSmgpSPTransmitHandler {
	
	protected BatchPool<Submit> batchPool;
	
	public BatchPoolSmgpSPSendHandler() {
		super.setLoginMode(Constants.LoginModes.SEND);
	}
	
	@Override
	public void setLoginMode(int loginMode) {
		throw new IllegalStateException("unsupport");
	}
	
	public void setBatchPool(BatchPool<Submit> batchPool) {
		this.batchPool = batchPool;
	}
	
	protected void returnQueuedSubmits(List<Submit> submits) {
		Assert.notNull(batchPool);
		batchPool.returnObjects(submits);
	}

	@Override
	protected List<Submit> takeQueuedSubmits() {
		return batchPool.takeObjects(getMaxOnceSubmits());
	}

	@Override
	protected boolean testQueuedSubmits() {
		return batchPool.isPooling();
	}

	@Override
	protected void doReceivedMessage(Deliver deliver) {
		throw new IllegalStateException("not support");
	}

	@Override
	protected void doReceivedReport(Deliver report) {
		throw new IllegalStateException("not support");
	}
	
	@Override
	protected void doDeliver(Request request, Response response) throws IOException {
		throw new PacketException("not support deliver");
	}
	
}
