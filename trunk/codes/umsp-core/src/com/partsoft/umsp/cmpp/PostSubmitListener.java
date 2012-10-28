package com.partsoft.umsp.cmpp;

import java.util.EventListener;

public interface PostSubmitListener extends EventListener {

	void onBeforePost(PostSubmitEvent event);
	
	void onPostSubmit(PostSubmitEvent event);
	
	void onSubmitResponse(SubmitResponseEvent event);
	
}
