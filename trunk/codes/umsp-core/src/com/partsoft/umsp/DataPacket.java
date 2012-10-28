package com.partsoft.umsp;

import java.io.Externalizable;
import java.io.Serializable;

public interface DataPacket extends Externalizable, Serializable {
	
	int getDataSize();
	
	int getBufferSize();
	
}
