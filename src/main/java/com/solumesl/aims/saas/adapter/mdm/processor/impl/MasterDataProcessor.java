package com.solumesl.aims.saas.adapter.mdm.processor.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.solumesl.aims.saas.adapter.constants.CommonConstants;
import com.solumesl.aims.saas.adapter.entity.job.JobStatus;
import com.solumesl.aims.saas.adapter.exception.ConstraintViolationException;
import com.solumesl.aims.saas.adapter.exception.NoStoreFoundException;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams.JobType;
import com.solumesl.aims.saas.adapter.mdm.mq.MQProducer;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.AssignStores;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.GetStores;
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
@Service
@Slf4j
@Primary
public class MasterDataProcessor extends Processor<RequestParams, Boolean> {


	static {
		ProcessorFactory.register(JobType.COPY, MasterDataProcessor.class);
	}


	@Autowired
	private   SaasRestService saasRestService;

	@Autowired
	private MQProducer messagePublishService;

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
			Action step1 = new GetStores(inputData, jobManager, saasRestService);

			Action step2 = new AssignStores(step1, requestParams, jobManager, messagePublishService);

			Step[] steps = new Step[]{step1, step2};

			executeSteps(steps);

			jobManager.writeJobStatus(jobId, JobStatus.MASTER_PARTIAL_COMPLETE);

		}  catch(NoStoreFoundException e) {
			KVPair additionalInfoMap = new KVPair();
			additionalInfoMap.put("message",e.getMessage());
			ClientResponse client = new ClientResponse(CommonConstants.SUCCESS_CODE, "Job Complete", additionalInfoMap );
			jobManager.writeJobStatus(jobId, JobStatus.MASTER_COMPLETE, client);
		}catch ( Exception e) {
		 
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
