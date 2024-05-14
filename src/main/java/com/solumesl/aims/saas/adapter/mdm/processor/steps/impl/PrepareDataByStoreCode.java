package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.solumesl.aims.saas.adapter.constants.SaasConstants;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.types.KVPair;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class PrepareDataByStoreCode extends Action<List<KVPair>> {

	private StepOutput<RequestParams> stepInput;
	private String storeCode;
	public PrepareDataByStoreCode(StepOutput<RequestParams> cexInputData, String storeCode) {
		this.stepInput = cexInputData;
		this.storeCode = storeCode;
	}
	@Override
	public boolean execute() throws Exception {
		setReturnValue(parseData());
		return true;
	}

	@SuppressWarnings("unchecked")
	public List<KVPair> parseData() {
		List<KVPair> data = stepInput.getOutput().getResult();
		Map<String, Object> formatMap = (Map<String, Object>) stepInput.getOutput().getParamByKey(SaasConstants.FORMAT);
		Set<String> set = new HashSet<> ();
		set.add("generateDate");set.add("lastModified");set.add("assignedLabel");
		List<KVPair> temp = data.stream().map(a->
		{ 
			a.keySet().removeAll(set);
			Map<String,String> mappingInfoBindings = (Map<String,String>)formatMap.get(SaasConstants.MAPPING_INFO);
			a.put(SaasConstants.STORE, storeCode);
			Map<String,String> keyVal = (Map<String, String>) a.get(SaasConstants.DATA);
			keyVal.put(mappingInfoBindings.get(SaasConstants.STORE), storeCode);
			return a;
		}).collect(Collectors.toList()); 

		return temp;

	}

}
