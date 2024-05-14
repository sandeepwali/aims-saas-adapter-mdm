package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;
import com.solumesl.aims.saas.adapter.util.SolumSaasUtil;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author baskarmohanasundaram
 *
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class TransformResult extends Action<List<Map>> {

	private StepOutput<List<Object>> result;

	public TransformResult(StepOutput<List<Object>> stepInput) {
		super();
		this.result = stepInput;
	}

	@Override
	public boolean execute() {
		setReturnValue(transformResult(result.getOutput()));
		return true;
	}
	private List<Map> transformResult(List<Object> result) {
		Objects.requireNonNull(result);
		return SolumSaasUtil.transformListObjToListMap(result);

	}
}
