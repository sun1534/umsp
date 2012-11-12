package com.partsoft.umsp;

import java.io.Externalizable;
import java.io.Serializable;

@SuppressWarnings("rawtypes")
public interface DataPacket extends Externalizable, Serializable, Comparable  {
	
	int getDataSize();
	
	int getBufferSize();
	
}
