package com.partsoft.umsp.sgip;

import java.util.List;

import com.partsoft.umsp.BatchPool;
import com.partsoft.utils.Assert;

public class BatchPoolSgipSPSendHandler extends AbstractSgipSPSendHandler {
	
	protected BatchPool<Submit> batchPool;
	
	public BatchPoolSgipSPSendHandler() {}
	
	public void setBatchPool(BatchPool<Submit> batchPool) {
		this.batchPool = batchPool;
	}

	@Override
	protected void returnQueuedSubmits(List<Submit> submits) {
		Assert.notNull(this.batchPool);
		this.batchPool.returnObjects(submits);
	}

	@Override
	protected List<Submit> takeQueuedSubmits() {
		Assert.notNull(this.batchPool);
		return this.batchPool.takeObjects(getMaxOnceSubmits());
	}

	@Override
	protected boolean testQueuedSubmits() {
		return this.batchPool.isPooling();
	}
	

}
