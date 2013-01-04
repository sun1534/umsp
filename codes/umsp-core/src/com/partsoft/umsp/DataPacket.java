package com.partsoft.umsp;

import java.io.Serializable;

@SuppressWarnings("rawtypes")
public interface DataPacket extends Serializable, Comparable  {
	
	int getDataSize();
	
	int getBufferSize();
	
}
