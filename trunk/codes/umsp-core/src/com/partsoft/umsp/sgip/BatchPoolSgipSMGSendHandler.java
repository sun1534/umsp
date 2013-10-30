package com.partsoft.umsp.sgip;

import java.io.Serializable;
import java.util.List;

import com.partsoft.umsp.BatchPool;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BatchPoolSgipSMGSendHandler extends AbstractSgipSMGSendHandler {
	
	protected BatchPool<? extends Serializable> batchPool;
	
	public void setBatchPool(BatchPool<? extends Serializable> batchPool) {
		this.batchPool = batchPool;
	}

	@Override
	protected void returnQueuedSubmits(List<MoForwardPacket> submits) {
		this.batchPool.returnObjects((List)submits);
	}

	@Override
	protected List<MoForwardPacket> takeQueuedSubmits(int count) {
		return (List<MoForwardPacket>) this.batchPool.takeObjects(count > getMaxOnceSubmits() ? getMaxOnceSubmits() : count);
	}

	@Override
	protected int testQueuedSubmits() {
		return this.batchPool.countPooling(getMaxOnceSubmits());
	}

}
