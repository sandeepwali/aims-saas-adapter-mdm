package com.solumesl.aims.saas.adapter.mdm.processor.steps.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.processor.steps.Action;
import com.solumesl.aims.saas.adapter.processor.steps.StepOutput;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author baskarmohanasundaram
 *
 */
@Slf4j
public class ProcessAsyncSaasResponse extends Action<List<Object>> {
	private static Logger logger = LoggerFactory.getLogger(ProcessAsyncSaasResponse.class);
	private StepOutput<List<CompletableFuture<?>>> stepInput;
	private long jobId;
	private long parentJobId;
	private JobManager jobManager;
	public ProcessAsyncSaasResponse(long parentJobId, long jobId, JobManager jobManager, StepOutput<List<CompletableFuture<?>>> stepInput) {
		super();
		this.stepInput = stepInput;
		this.jobManager = jobManager;
		this.jobId = jobId;
		this.parentJobId = parentJobId;
	}

	@Override
	public boolean execute() throws Exception {
		setReturnValue(processFutureResponse(stepInput.getOutput()));
		return true;
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
								jobManager.writeJobSummary(parentJobId,jobId,response);
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
			logger.error("Error in processFutureResponse-->"+ ExceptionUtils.getStackTrace(e));
			throw e;
		}
	}
}
