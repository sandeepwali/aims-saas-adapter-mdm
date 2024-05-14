package com.solumesl.aims.saas.adapter.mdm.processor.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.solumesl.aims.saas.adapter.constants.CommonConstants;
import com.solumesl.aims.saas.adapter.constants.SaasConstants;
import com.solumesl.aims.saas.adapter.exception.ConstraintViolationException;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams.JobType;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.GenerateClientResponse;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.PrepareDataByStoreCode;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.ProcessAsyncSaasResponse;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.TransformResult;
import com.solumesl.aims.saas.adapter.mdm.processor.steps.impl.UploadArticleToSaasAsync;
import com.solumesl.aims.saas.adapter.model.ClientResponse;
import com.solumesl.aims.saas.adapter.processor.Processor;
import com.solumesl.aims.saas.adapter.processor.factory.ProcessorFactory;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.Step;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.service.MessageService;
import com.solumesl.aims.saas.adapter.service.SaasRestService;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author baskarmohanasundaram
 *
 */
@Slf4j
@Service
public class ArticleDataProcessor extends Processor<RequestParams, ClientResponse> {


	static {
		ProcessorFactory.register(JobType.ARTICLE, ArticleDataProcessor.class);
	}


	@Value("${solum.saas.server.api.batchsplit}")
	private int apiBatchSplit;


	@Autowired
	private   SaasRestService saasRestService;


	@Autowired
	private MessageService msgService;

	@Autowired
	private JobManager jobManager;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ClientResponse processRequest(RequestParams requestParams) throws ConstraintViolationException {

		String company = requestParams.getStoreDetail().getCompany();

		Optional<String> store = Optional.of(requestParams.getStoreDetail().getStore());
		if(!store.isPresent()) {
			throw new ConstraintViolationException(SaasConstants.STORE);
		}



		try {


			StepOutput cexInputData = new StepOutput() {
				@Override
				public Object getOutput() {
					return requestParams;
				}
			};
			Action initialStep  = new PrepareDataByStoreCode(cexInputData, store.get());

			Action step1 =  new UploadArticleToSaasAsync(initialStep, company, store.get(), saasRestService, apiBatchSplit);

			Action step2 =  new ProcessAsyncSaasResponse(requestParams.getParentJobId(), requestParams.getJobId(), jobManager, step1);

			Action step3 =  new TransformResult(step2);

			Action finalStep =  new GenerateClientResponse(step3, msgService,store.get());

			Step[] steps = new Step[]{ initialStep, step1, step2, step3, finalStep};

			executeSteps(steps);
			
			return (ClientResponse) finalStep.getOutput();

		}    catch ( Exception e) {
			return new ClientResponse(CommonConstants.FAILURE_CODE, getMessage("error.fileprocess"));
		}

	}


 





}
