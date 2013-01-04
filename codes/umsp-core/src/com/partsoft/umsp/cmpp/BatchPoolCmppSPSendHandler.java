package com.partsoft.umsp.cmpp;

import java.util.List;

import com.partsoft.umsp.BatchPool;
import com.partsoft.utils.Assert;

/**
 * 长连接发送通道
 * @author neeker
 *
 */
public class BatchPoolCmppSPSendHandler extends AbstractCmppSPTransmitHandler {
	
	protected BatchPool<Submit> batchPool;
	
	public BatchPoolCmppSPSendHandler() {}
	
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
		throw new IllegalStateException("unsupport");
	}

	@Override
	protected void doReceivedReport(Deliver report) {
		throw new IllegalStateException("unsupport");
	}
	
}

