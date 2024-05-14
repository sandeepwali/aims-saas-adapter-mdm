package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.List;

import com.solumesl.aims.saas.adapter.entity.job.JobStatus;
import com.solumesl.aims.saas.adapter.exception.NoStoreFoundException;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams.JobType;
import com.solumesl.aims.saas.adapter.mdm.mq.MQProducer;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.util.SolumSaasUtil;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class AssignStores extends Action<Object> {
	private StepOutput<List<String>> stepInput;
	private RequestParams requestParams;
	private JobManager jobManager;
	private MQProducer messagePublishService;
	@Override
	public boolean execute() throws Exception {
		assignStore();
		return true;
	}
	
	private void assignStore() throws Exception {
		List<String> storeIds = stepInput.getOutput();
		
		if(SolumSaasUtil.isNotEmpty(storeIds)) {

			//for each store we need to duplicate request with the store code, for each store a new child job will be created and request is posted to MQ
			storeIds.forEach(storeId->{

				assignStore(requestParams, storeId);

				assignNewChildJobToQueue(requestParams);//log the job in DB and push it to MQ

			});
		}else{

			log.info("No store found for the company, {}  ", requestParams.getStoreDetail());
			String displayMessage = "No store found for the company - "+ requestParams.getStoreDetail().getCompany() + ", Country -" +requestParams.getStoreDetail().getCountry()
					+ " & Optional params - "+ requestParams.getStoreDetail().getQueryParams()  ;
			throw new NoStoreFoundException(displayMessage);
		}
		
	}
	public AssignStores(StepOutput<List<String>> stepInput, RequestParams requestParams, JobManager jobManager, MQProducer messagePublishService) {
		super();
		this.stepInput = stepInput;
		this.requestParams = requestParams;
		this.jobManager = jobManager;
		this.messagePublishService = messagePublishService;
	}

	private void assignStore(RequestParams requestParams, Object a) {
		requestParams.getStoreDetail().setStore((String) a);
	}

	private void assignNewChildJobToQueue(RequestParams requestParams) {
		try {
			long newJobId = requestParams.assignCurrentJobIdAsParentAndGenerateChildId();

			requestParams.setJobType(JobType.ARTICLE);

			jobManager.writeJobStatus(newJobId, JobStatus.CHILD_PRE_ACCEPT);

			messagePublishService.sendArticle(requestParams);

			jobManager.writeJobStatus(newJobId, JobStatus.CHILD_ACCEPTED);

			jobManager.updateChild(requestParams.getParentJobId(), requestParams.getChildJobs());
		} catch (Exception e) {
			 log.error("Error in assigning child Job {}", requestParams); 
		}

	}

}
