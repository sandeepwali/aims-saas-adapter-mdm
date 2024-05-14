package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.solumesl.aims.saas.adapter.constants.SaasConstants;
import com.solumesl.aims.saas.adapter.entity.job.JobStatus;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams.JobType;
import com.solumesl.aims.saas.adapter.mdm.mq.MQProducer;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.service.SaasRestService;
import com.solumesl.aims.saas.adapter.types.KVPair;
import com.solumesl.aims.saas.adapter.util.SolumSaasUtil;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class QueryArticlesByPagingAndPushToMq extends Action<List<Object>> {

	@SuppressWarnings("rawtypes")
	private StepOutput<Map> stepInput;
	private Integer apiBatchSplit;
	private SaasRestService saasRestService;
	public QueryArticlesByPagingAndPushToMq(StepOutput<Map> stepInput, RequestParams requestParams,
			SaasRestService saasRestService, JobManager jobManager, MQProducer messagePublishService,
			Integer apiBatchSplit) {
		super();
		this.stepInput = stepInput;
		this.requestParams = requestParams;
		this.saasRestService = saasRestService;
		this.jobManager = jobManager;
		this.messagePublishService = messagePublishService;
		this.apiBatchSplit = apiBatchSplit;
	}

	private RequestParams requestParams;
	private JobManager jobManager;
	private MQProducer messagePublishService;
	@Override
	public boolean execute() throws Exception {
		setReturnValue(queryArticlesByPaging());
		return true;
	}
 
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Object> queryArticlesByPaging() {
		
		List<Object> ids = null;
		Map articleCountMap = stepInput.getOutput();
		String company = requestParams.getStoreDetail().getCompany();
		String fromStore = (String) requestParams.getResult().get(0).get("fromStore");
		long jobId = requestParams.getJobId();
		Integer runCount = (Integer) articleCountMap.get(SaasConstants.TOTAL_ARTICLE_COUNT);
		int pages = Math.floorDiv(runCount, apiBatchSplit);
		List<CompletableFuture<?>> results =  new ArrayList<CompletableFuture<?>>();
		for(int i = 0; i <= pages && runCount > 0 ; i++) {
			try {
				Map<String, String> tempQueryMap = new HashMap<>();
				tempQueryMap.put("page",String.valueOf(i));
				tempQueryMap.put("size",String.valueOf(apiBatchSplit));
				CompletableFuture<?> articles = saasRestService.getArticles(company, fromStore, tempQueryMap);
				CompletableFuture<Object> cPchildJob = articles.thenApply(article->{
					
					Map<String,Object> data = (Map<String, Object>) article;
					if(SolumSaasUtil.isEmpty(data)) {
						return null;
					}
					List<KVPair> articleList = (List<KVPair>) data.get(SaasConstants.ARTICLES);
					RequestParams childRequestParam = new RequestParams(articleList, requestParams.getStoreDetail(), jobId);
					childRequestParam.addAdditionalParam(SaasConstants.FORMAT, requestParams.getParamByKey(SaasConstants.FORMAT));
					assignNewChildJobToQueue(childRequestParam, requestParams.getChildJobs());
					return childRequestParam.getJobId();
				});
				results.add(cPchildJob);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			ids = processFutureResponse(results);
			
			if(SolumSaasUtil.isNotEmpty(ids)) {
				log.info("childJobIds-->"+ requestParams.getChildJobs());
				jobManager.writeJobStatus(jobId, JobStatus.MASTER_PARTIAL_COMPLETE);
			}else {
				jobManager.writeJobStatus(jobId, JobStatus.MASTER_ERROR);
			}
		} catch (Exception e) {
			jobManager.writeJobStatus(jobId, JobStatus.MASTER_ERROR);
		}
		return ids;
	}



	private void assignNewChildJobToQueue(RequestParams requestParams, Set<Long> childJobs) {

		long newJobId = requestParams.assignCurrentJobIdAsParentAndGenerateChildId();

		childJobs.add(newJobId);
		
		requestParams.setJobType(JobType.ARTICLE);
		
		jobManager.writeJobStatus(newJobId, JobStatus.CHILD_PRE_ACCEPT);

		messagePublishService.sendArticle(requestParams);

		jobManager.writeJobStatus(newJobId, JobStatus.CHILD_ACCEPTED);

		jobManager.updateChild(requestParams.getParentJobId(), childJobs);

	}
	
	@SuppressWarnings("unchecked")
	private List<Object> processFutureResponse(List<CompletableFuture<?>> result) throws Exception {
		try {
			
			CompletableFuture<Void> allFutures = CompletableFuture.allOf(result.toArray(new CompletableFuture[result.size()]));
			
			CompletableFuture<List<Object>> allCompletableFuture = allFutures.thenApply(future -> {
				return result.stream()
						.map(completableFuture -> {

							Object response = null;
							try {
								response = completableFuture.join();
							} catch (Exception e) {
							}

							return response;
						})
						.collect(Collectors.toList());
			});

			CompletableFuture<?> completableFuture = allCompletableFuture.thenApply(data -> {
				return data.stream().map(a->a).collect(Collectors.toList());

			});
			List<Object> resultList = (List<Object>) completableFuture.get();
			resultList.removeAll(Collections.singleton(null));
			return resultList;
		} catch (Exception e) {
			log.error("Error in processFutureResponse-->"+ ExceptionUtils.getStackTrace(e));
			throw e;
		}
	}
}
