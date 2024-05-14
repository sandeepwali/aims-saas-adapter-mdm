package com.solumesl.aims.saas.adapter.mdm.processor.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.solumesl.aims.saas.adapter.constants.CommonConstants;
import com.solumesl.aims.saas.adapter.entity.job.JobStatus;
import com.solumesl.aims.saas.adapter.exception.ConstraintViolationException;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams.JobType;
import com.solumesl.aims.saas.adapter.mdm.mq.MQProducer;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.CollectArticleInfo;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.QueryArticlesByPagingAndPushToMq;
import com.solumesl.aims.saas.adapter.model.ClientResponse;
import com.solumesl.aims.saas.adapter.processor.Processor;
import com.solumesl.aims.saas.adapter.processor.factory.ProcessorFactory;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.Step;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.service.SaasRestService;
import com.solumesl.aims.saas.adapter.types.KVPair;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author baskarmohanasundaram
 *
 */
@Slf4j
@Service
public class StoreSyncProcessor extends Processor<RequestParams, Boolean> {



	@Autowired
	private MQProducer messagePublishService;

	static {
		ProcessorFactory.register(JobType.SYNC, StoreSyncProcessor.class);
	}


	@Value("${solum.saas.server.api.batchsplit}")
	private int apiBatchSplit;


	@Autowired
	private   SaasRestService saasRestService;



	@Autowired
	private JobManager jobManager;
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Boolean processRequest(RequestParams requestParams) throws ConstraintViolationException {
		long jobId = requestParams.getJobId();
		try {


			StepOutput inputData = new StepOutput() {
				@Override
				public Object getOutput() {
					return requestParams;
				}
			};
			Action step1 = new CollectArticleInfo(inputData, saasRestService);

			Action step2 = new QueryArticlesByPagingAndPushToMq(step1, requestParams, saasRestService, jobManager, messagePublishService, apiBatchSplit);

			Step[] steps = new Step[]{step1, step2};

			executeSteps(steps);

		} catch ( Exception e) {
			
			writeErrorStatus(jobId, e);
		}
		return true;

	}
	private void writeErrorStatus(long jobId, Exception e) {
		KVPair additionalInfoMap = new KVPair();
		additionalInfoMap.put("error",e.getMessage());
		ClientResponse client = new ClientResponse(CommonConstants.FAILURE_CODE, "Job Failed", additionalInfoMap );
		jobManager.writeJobStatus(jobId, JobStatus.MASTER_ERROR, client);
	}





}
