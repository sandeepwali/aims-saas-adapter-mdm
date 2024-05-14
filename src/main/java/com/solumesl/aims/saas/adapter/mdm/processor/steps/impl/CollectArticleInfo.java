package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.solumesl.aims.saas.adapter.constants.SaasConstants;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.service.SaasRestService;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class CollectArticleInfo extends Action<Map<String, Object>> {
	private StepOutput<RequestParams> stepInput;
	private   SaasRestService saasRestService;
	public CollectArticleInfo(StepOutput<RequestParams> stepInput,   SaasRestService saasRestService) {
		super();
		this.stepInput = stepInput;
		this.saasRestService = saasRestService;
	}
	@Override
	public boolean execute() throws Exception {
		setReturnValue(collectArticleInfo());
		return true;
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, Object> collectArticleInfo() throws Exception {
		Map articleCountMap = null;
		String fromStore = null;
		try {
			RequestParams requestParams = stepInput.getOutput();
			String company = requestParams.getStoreDetail().getCompany();
			fromStore = (String) requestParams.getResult().get(0).get("fromStore");
			String toStore = (String) requestParams.getResult().get(0).get("toStore");
			requestParams.getStoreDetail().setStore(toStore);
			CompletableFuture<?> format = queryArticleFormat(company);
			Map<String, String> queryMap = new HashMap<>();
			try {
				addArticleFormatToRequest(requestParams, format);
			} catch (InterruptedException | ExecutionException e) {
				log.error("Unable to fetch Article Format from Saas"+  ExceptionUtils.getStackTrace(e));
				throw new Exception("Unable to fetch Article Format from Saas");
			} 
			CompletableFuture<?> result = saasRestService.getArticlesCount(company, fromStore, queryMap);
			articleCountMap = (Map<String, Object>) result.get();
		} catch (InterruptedException | ExecutionException e1) {
			log.error("Unable to fetch Articles count"+ ExceptionUtils.getStackTrace(e1));
			throw new Exception("Unable to fetch Articles  for the store "+ fromStore);
		}  

		return articleCountMap;

	}

	private CompletableFuture<?> queryArticleFormat(String company) {
		Map<String, String> articleFormatQueryMap = new HashMap<>();
		articleFormatQueryMap.put(SaasConstants.COMPANY, company);
		CompletableFuture<?> format = saasRestService.getArticleFormat(articleFormatQueryMap);
		return format;
	}

	private void addArticleFormatToRequest(RequestParams requestParams, CompletableFuture<?> format)
			throws InterruptedException, ExecutionException {
		@SuppressWarnings("unchecked")

		Map<String,Object> formatMap =   (Map<String,Object>) format.get();
		requestParams.addAdditionalParam(SaasConstants.FORMAT, formatMap);
	}


}
