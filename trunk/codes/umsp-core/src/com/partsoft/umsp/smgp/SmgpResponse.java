package com.partsoft.umsp.smgp;

import java.io.IOException;
import java.io.OutputStream;

import com.partsoft.umsp.Response;
import com.partsoft.umsp.packet.PacketOutputStream;

public class SmgpResponse implements com.partsoft.umsp.Response {
	
	public static final String ARG_RESPONSE = "smgp.response";

	private com.partsoft.umsp.Response _proxy;

	private PacketOutputStream _datastream;

	public SmgpResponse(com.partsoft.umsp.Response proxy) {
		this._proxy = proxy instanceof SmgpResponse ? ((SmgpResponse) proxy)._proxy : proxy;
	}
	
	protected void setWrapper(Response proxy) {
		this._proxy = proxy;
	}
	
	public Response getWrapper() {
		return this._proxy;
	}
	
	public void setContentLength(int paramInt) {
		_proxy.setContentLength(paramInt);
	}

	public void setBufferSize(int paramInt) {
		_proxy.setBufferSize(paramInt);
	}

	public int getBufferSize() {
		return _proxy.getBufferSize();
	}

	public void flushBuffer() throws IOException {
		_proxy.flushBuffer();
	}

	public void finalBuffer() throws IOException {
		_proxy.finalBuffer();
	}

	public void resetBuffer() {
		_proxy.resetBuffer();
	}

	public boolean isCommitted() {
		return _proxy.isCommitted();
	}

	public void reset() {
		_proxy.reset();
	}

	public OutputStream getOutputStream() throws IOException {
		return _proxy.getOutputStream();
	}
	
	
	public void writeDataPacket(SmgpDataPacket packet) throws IOException {
		PacketOutputStream pakcetOutputStream = getPacketStream();
		pakcetOutputStream.writeInt(packet.getBufferSize());
		packet.writeExternal(pakcetOutputStream);
	}

	public void flushDataPacket(SmgpDataPacket packet) throws IOException {
		writeDataPacket(packet);
		flushBuffer();
	}	

	public PacketOutputStream getPacketStream() throws IOException {
		synchronized (this) {
			if (_datastream == null) {
				_datastream = new PacketOutputStream(getOutputStream());
			}
			return _datastream;
		}
	}

}