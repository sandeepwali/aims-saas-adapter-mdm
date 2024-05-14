package com.solumesl.aims.saas.adapter.mdm.mq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class MQProducer {
	@Autowired
	private StreamBridge streamBridge;

	
	public void sendMaster(Object args) {
		streamBridge.send("master-out-0", args);
	}
	 
	
	public void sendArticle(Object args) {
		streamBridge.send("article-out-0", args);
	}
	 
	 
}
