package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.service.SaasRestService;
import com.solumesl.aims.saas.adapter.types.KVPair;
import com.solumesl.aims.saas.adapter.util.SolumSaasUtil;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author baskarmohanasundaram
 *
 */
@Slf4j
public class UploadArticleToSaasAsync extends Action<List<CompletableFuture<?>>> {

	private int apiBatchSplit;

	private   SaasRestService saasRestService;

	public UploadArticleToSaasAsync(StepOutput<List<KVPair> > stepInput, String company, String store, SaasRestService saasRestService, int apiBatchSplit) {
		super();
		this.stepInput = stepInput;
		this.company = company;
		this.store = store;
		this.saasRestService =  saasRestService;
		this.apiBatchSplit = apiBatchSplit;
	}
	private StepOutput<List<KVPair> > stepInput;
	private String company;
	private String store;
 

	@Override
	public boolean execute() throws Exception {
		setReturnValue(uploadDataToSaas(stepInput.getOutput(), company, store));
		return true;
	}
	private  List<CompletableFuture<?>> uploadDataToSaas(List<KVPair> dataList, String company, String store) throws    Exception { 

		List<CompletableFuture<?>> result = new ArrayList<>();
		if(SolumSaasUtil.isNotEmpty(dataList)){
			List<List<KVPair>> chunks = Lists.partition(dataList, apiBatchSplit);
			for(List<KVPair> dataChunks:chunks) {
				result.add(saasRestService.uploadArticles(company, store, dataChunks));
			}
		}
		return result;
	}
}
