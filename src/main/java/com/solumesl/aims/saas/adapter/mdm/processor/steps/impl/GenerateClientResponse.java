package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.solumesl.aims.saas.adapter.constants.CommonConstants;
import com.solumesl.aims.saas.adapter.model.ClientResponse;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.service.MessageService;
import com.solumesl.aims.saas.adapter.types.KVPair;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author baskarmohanasundaram
 *
 */
@Slf4j
public class GenerateClientResponse extends Action<ClientResponse> {

	private StepOutput<List<Map>>  previousStep;
	private MessageService msgService;
	private String store;
	public GenerateClientResponse(StepOutput<List<Map>> stepInput, MessageService msgService, String store ) {
		super();
		this.previousStep = stepInput;
		this.msgService = msgService;
		this.store = store;
	}


	@Override
	public boolean execute() {
		setReturnValue(prepareClientResponse(previousStep.getOutput()));
		return true;
	}

	private ClientResponse prepareClientResponse(@SuppressWarnings("rawtypes") List<Map> resultsList) {

		Objects.requireNonNull(resultsList);

		KVPair additionalInfoMap = new KVPair();

		additionalInfoMap.put("processedRecords",getTotalByFieldValue(resultsList, "validCount"));
		additionalInfoMap.put("failedRecords",getTotalByFieldValue(resultsList, "invalidCount"));
		additionalInfoMap.put("store",store);

		return new ClientResponse(CommonConstants.SUCCESS_CODE, this.msgService.getMessage("info.fileprocess"), additionalInfoMap );
	}

	private long getTotalByFieldValue(List<Map> resultsList, String fieldName) {
		return resultsList.stream().filter(Objects::nonNull).mapToLong(a->(Integer) a.get(fieldName)).sum();

	}
}
