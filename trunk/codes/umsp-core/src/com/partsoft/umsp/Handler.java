package com.partsoft.umsp;

import java.io.IOException;

public interface Handler extends LifeCycle {
	
	//请求
	public static final int REQUEST = 1;
	
	//等待超时
	public static final int TIMEOUT = 2;
	
	public void handle(String protocol, Request request, Response response, int dispatch) throws IOException;

	public void setOrigin(OriginHandler server);

	public OriginHandler getOrigin();

	public void destroy();

}
