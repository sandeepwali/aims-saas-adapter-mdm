package com.solumesl.aims.saas.adapter.mdm.mq;

import java.io.IOException;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import com.rabbitmq.client.Channel;
import com.solumesl.aims.saas.adapter.constants.CommonConstants;
import com.solumesl.aims.saas.adapter.entity.job.JobStatus;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.model.ClientResponse;
import com.solumesl.aims.saas.adapter.processor.factory.ProcessorFactory;

import lombok.extern.slf4j.Slf4j;
@Configuration
@Slf4j
public class MQConsumer   {

	@Autowired
	private JobManager jobManager;


	@SuppressWarnings("unchecked")
	@Bean 
	public Consumer<Message<RequestParams>> article() { 
		return (data) ->
		{

			RequestParams requestParam =  data.getPayload(); 

			MessageHeaders headers = data.getHeaders();

			Channel channel = (Channel) headers.get("amqp_channel"); 
			Long deliveryTag = (Long) headers.get("amqp_deliveryTag");

			if(requestParam == null) {
				rejectMessage(requestParam, channel, deliveryTag);
				return;
			}
			try { 

				jobManager.writeJobStatus(requestParam.getParentJobId(), requestParam.getJobId(), JobStatus.CHILD_PROECESSING); 

				ClientResponse response = (ClientResponse) ProcessorFactory.getInstance(requestParam.getJobType().toString()).processRequest(requestParam);

				if(CommonConstants.SUCCESS_CODE.equals(response.getResponseCode())) {
					jobManager.writeJobStatus(requestParam.getParentJobId(),requestParam.getJobId(), JobStatus.CHILD_COMPLETE, response);
				}else {
					jobManager.writeJobStatus(requestParam.getParentJobId(),requestParam.getJobId(), JobStatus.CHILD_ERROR, response);
				}
					
				acknowledgeMessage(channel, deliveryTag,requestParam); 

			}catch(Exception e) {
				log.error("Error in processing for the job {},{}",requestParam.getJobId(), e); 
				jobManager.writeJobStatus(requestParam.getParentJobId(),requestParam.getJobId(), JobStatus.CHILD_ERROR);
				rejectMessage(requestParam, channel, deliveryTag); }
		};

	}


	@SuppressWarnings("unchecked")
	@Bean
	public Consumer<Message<RequestParams>> master() {
		return (data) -> {
			RequestParams requestParam = null;
			Channel channel = null;
			Long deliveryTag = 0L;
			try {
				requestParam = data.getPayload();

				if(requestParam == null) {
					rejectMessage(requestParam, channel, deliveryTag);
					return;
				}
				jobManager.writeJobStatus(requestParam.getJobId(), JobStatus.MASTER_PROCESSING);
				MessageHeaders headers = data.getHeaders();

				channel = (Channel) headers.get("amqp_channel");

				deliveryTag = (Long) headers.get("amqp_deliveryTag");

				ProcessorFactory.getInstance(requestParam.getJobType().toString()).processRequest(requestParam);

				acknowledgeMessage(channel, deliveryTag,requestParam);
			} catch (Exception e) {
				log.error("Error in processing for the job {},{}",requestParam.getJobId(), e);
				rejectMessage(requestParam, channel, deliveryTag);
			}	
		};

	}


	private void rejectMessage(RequestParams requestParam, Channel channel, Long deliveryTag) {
		try {
			channel.basicNack(deliveryTag, false, false);
		} catch (IOException e1) {
			log.error("Error while rejecting message for job id :{}", requestParam.getJobId());
		}
	}

	private void acknowledgeMessage(Channel channel, Long deliveryTag, RequestParams requestParam) throws IOException {
		try {
			channel.basicAck(deliveryTag,  false);
		} catch (IOException e) {
			log.error("Error while acknowledging message for job id :{}", requestParam.getJobId());
		}
	}

}

