package com.solumesl.aims.saas.adapter.mdm.scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.solumesl.aims.saas.adapter.constants.CommonConstants;
import com.solumesl.aims.saas.adapter.constants.SaasConstants;
import com.solumesl.aims.saas.adapter.entity.job.Job;
import com.solumesl.aims.saas.adapter.entity.job.JobStatus;
import com.solumesl.aims.saas.adapter.entity.job.JobSummary;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.model.ClientResponse;
import com.solumesl.aims.saas.adapter.service.MessageService;
import com.solumesl.aims.saas.adapter.types.KVPair;
import com.solumesl.aims.saas.adapter.util.SolumSaasUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BgTask {

	@Value("${solum.saas.server.api.batchsplit}")
	private int apiBatchSplit;



	@Autowired
	private MessageService msgService;

	@Autowired
	private JobManager jobManager;

	@Scheduled(fixedDelayString  = "${solum.saas.bgtask.fixeddelay}", initialDelay = 1000)
	public void updateMasterStatus() {
		List<Job> activeParentJobs = jobManager.getAllActiveMasterJob(JobStatus.MASTER_PARTIAL_COMPLETE).orElse(new ArrayList<>());
		activeParentJobs.forEach(job->{
			try {
				List<Job> childJobResult = jobManager.findAll(job.getChildJobs()).orElse(new ArrayList<>());
				boolean isAllChildJobCompleted = childJobResult.stream().allMatch(childJob->childJob.getJobStatus().equals(JobStatus.CHILD_COMPLETE));
				if(isAllChildJobCompleted) {
					log.info("Master complete,{}"+job.getJobId());
					Map<String, Object> additionalInfoMap = prepareChildJobSumaary(job, childJobResult);
					ClientResponse clientResponse = new ClientResponse(CommonConstants.SUCCESS_CODE, this.msgService.getMessage("info.fileprocess"), additionalInfoMap );
					jobManager.writeJobStatus(job.getJobId(), JobStatus.MASTER_COMPLETE, clientResponse);
				}else if(childJobResult.stream().anyMatch(childJob-> (childJob.getJobStatus().equals(JobStatus.CHILD_ERROR))) && !childJobResult.stream().anyMatch(childJob-> (childJob.getJobStatus().equals(JobStatus.CHILD_PROECESSING)))) {
					log.info("Master job - {} has some child job errors hence changing the status to error ",job.getJobId());
					Map<String, Object> additionalInfoMap = prepareChildJobSumaary(job, childJobResult);
					ClientResponse clientResponse = new ClientResponse(CommonConstants.FAILURE_CODE, this.msgService.getMessage("info.fileprocess"), additionalInfoMap );
					jobManager.writeJobStatus(job.getJobId(), JobStatus.MASTER_ERROR, clientResponse);
				}
			}catch(Exception e) {
				log.error("Error in update Master Status,{}, exception {}",job, ExceptionUtils.getStackTrace(e));
			}
		});
	}

	private Map<String, Object> prepareChildJobSumaary(Job job, List<Job> childJobResult) {
		List<JobSummary> summaryList = jobManager.readJobSummaryByParent(job.getJobId());
		Map<String, Object> additionalInfoMap = addAdditionalInfo(childJobResult, summaryList);
		return additionalInfoMap;
	}

	private Map<String, Object> addAdditionalInfo(List<Job> childJobResult, List<JobSummary> summaryList) {
		KVPair additionalInfoMap = new KVPair();
		try {
			additionalInfoMap.put("processedRecords",calculate(summaryList, o -> o.getValidCount()));
			additionalInfoMap.put("failedRecords",calculate(summaryList, o -> o.getInvalidCount()));
			Map<String, List<Map<String, Object>>> storesData = mergeChildJobDataByStoreWise(childJobResult);
			int totalStores = storesData == null ? 0: storesData.keySet().size();
			additionalInfoMap.put("totalStores",totalStores); 
			additionalInfoMap.put(SaasConstants.STORES, combineStoreInfos(storesData));
		} catch (Exception e) {
			log.error("Error in addAdditionalInfo, exception {}", ExceptionUtils.getStackTrace(e));
		}
		return additionalInfoMap;
	}

	private Object combineStoreInfos(Map<String, List<Map<String, Object>>> calculateTotalStores) {
		List<KVPair> resultList = new ArrayList<KVPair>();
		try {
			if(SolumSaasUtil.isNotEmpty(calculateTotalStores)) {
				calculateTotalStores.keySet().forEach(a->{
					KVPair additionalInfoMap = new KVPair();
					additionalInfoMap.put("processedRecords",getTotalByFieldValue(
							calculateTotalStores.get(a), "processedRecords"));
					additionalInfoMap.put("failedRecords",getTotalByFieldValue( calculateTotalStores.get(a),
							"failedRecords")); additionalInfoMap.put("store",a);
							resultList.add(additionalInfoMap);
				});
			}
		} catch (Exception e) {
			log.error("Error in combineStoreInfos, exception {}", ExceptionUtils.getStackTrace(e));
		}
		return resultList;
	}

	private Map<String, List<Map<String, Object>>>  mergeChildJobDataByStoreWise(List<Job> childJobResult) {
		try {
			List<Map<String, Object>> rxData = childJobResult.stream().filter(a->JobStatus.CHILD_COMPLETE.equals(a.getJobStatus())).map(childjob->extracted(childjob)).collect(Collectors.toList());


			Map<String, List<Map<String, Object>>> res = rxData.stream().collect(Collectors.groupingBy(map -> map.get("store").toString(),
					Collectors.mapping(map -> map,
							Collectors.toList())));
			return res;
		} catch (Exception e) {
			log.error("Error in mergeChildJobDataByStoreWise, exception {}", ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extracted(Job childjob) {
		return (Map<String, Object>)childjob.getResult().getAdditionalInfo();
	}
	
	
	private long getTotalByFieldValue(List<Map<String,Object>> resultsList, String fieldName) {
		return resultsList.stream().filter(Objects::nonNull).mapToLong(a->(Long) a.get(fieldName)).sum();

	}
	private long calculate(List<JobSummary> summaryList, ToLongFunction<? super JobSummary> function) {

		return summaryList.stream().filter(Objects::nonNull).mapToLong(function).sum();
	}


}
