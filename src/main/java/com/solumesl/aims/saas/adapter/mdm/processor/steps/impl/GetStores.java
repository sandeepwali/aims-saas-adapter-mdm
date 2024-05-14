package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.solumesl.aims.saas.adapter.constants.SaasConstants;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.service.SaasRestService;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class GetStores extends Action<List<Object>> {
	private StepOutput<RequestParams> stepInput;
	private   SaasRestService saasRestService;
	@Override
	public boolean execute() throws Exception {
		List<Object> storeList = getStoreList();
		setReturnValue(storeList);
		return true;
	}

	public GetStores(StepOutput<RequestParams> stepInput, JobManager jobManager, SaasRestService saasRestService ) {
		this.stepInput = stepInput;
		this.saasRestService = saasRestService;
	}


	private List<Object> getStoreList() throws Exception{

		RequestParams requestParams = stepInput.getOutput() ;


		String company = requestParams.getStoreDetail().getCompany();

		String country = requestParams.getStoreDetail().getCountry();


		Map<String, String> queryMap = new HashMap<>();

		queryMap.put(SaasConstants.COUNTRY, country);

		queryMap.putAll(requestParams.getStoreDetail().getQueryParams());//if any additional query param, e.g city, region
		int page = 0;
		queryMap.put("page",String.valueOf(page));
		int  size = 200;
		queryMap.put("size",String.valueOf(size));
		CompletableFuture<?> stores = queryStore(company, queryMap);// query all available stores for the specific company with query match

		CompletableFuture<?> format = queryArticleFormat(company);//get article format for the company, needed for preparing data

		try {

			addArticleFormatToRequest(requestParams, format);// add the article format to each request for processing data

			List<Object> storeIds = getStores(stores);//get stores list from async call

			return storeIds;

		} catch (InterruptedException  | ExecutionException e) {
			log.error("Error in getStoreList, {}  ", ExceptionUtils.getStackTrace(e));
			throw e;
		}

	}




	private CompletableFuture<?> queryArticleFormat(String company) {
		Map<String, String> articleFormatQueryMap = new HashMap<>();
		articleFormatQueryMap.put(SaasConstants.COMPANY, company);
		CompletableFuture<?> format = saasRestService.getArticleFormat(articleFormatQueryMap);
		return format;
	}

	private CompletableFuture<?> queryStore(String company, Map<String,String> queryMap) {

		CompletableFuture<?> stores = saasRestService.getStore(company, queryMap);
		return stores;
	}

	private List<Object> getStores(CompletableFuture<?> stores) throws InterruptedException, ExecutionException {
		@SuppressWarnings("unchecked")
		Map<String,Object> storesMap =   (Map<String,Object>) stores.get();
		List<Object> storeIds = getStoreIds(Optional.ofNullable(storesMap));
		return storeIds;
	}

	private void addArticleFormatToRequest(RequestParams requestParams, CompletableFuture<?> format)
			throws InterruptedException, ExecutionException {
		@SuppressWarnings("unchecked")
		Map<String,Object> formatMap =   (Map<String,Object>) format.get();
		requestParams.addAdditionalParam(SaasConstants.FORMAT, formatMap);
	}

	private List<Object> getStoreIds(Optional<Map<String, Object>> storesMap) {
		if(storesMap.isPresent()) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> storesList = (List<Map<String,Object>>) storesMap.get().get(SaasConstants.STORES);
			List<Object> storeIds = getStoreList(storesList);
			return storeIds;
		}else {
			return null;
		}

	}

	private List<Object> getStoreList(List<Map<String, Object>> storesList) {
		List<Object> storeIds = storesList.stream().map(a->a.get(SaasConstants.STORE)).collect(Collectors.toList());
		return storeIds;
	}
}
