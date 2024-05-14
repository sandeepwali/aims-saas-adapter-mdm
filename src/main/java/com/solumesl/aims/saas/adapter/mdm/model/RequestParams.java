package com.solumesl.aims.saas.adapter.mdm.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.types.KVPair;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestParams implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String PARENT = "parentId";
	private List<KVPair> result;
	public RequestParams(List<KVPair> result, StoreDetail storeDetail, long jobId) {
		super();
		this.result = result;
		this.storeDetail = storeDetail;
		this.jobId = jobId;
	}
	private StoreDetail storeDetail;
	private long jobId;
	private KVPair additionalParams = new KVPair();
	private JobType jobType = JobType.COPY;
	@JsonIgnore
	private Set<Long> childJobs = new LinkedHashSet<Long>();
	
	public long assignCurrentJobIdAsParentAndGenerateChildId() {
		additionalParams.putIfAbsent(PARENT, jobId);
		 jobId = JobManager.generateJobId();
		childJobs.add(jobId);
		return jobId;
	}
	
	public long getParentJobId() {
		return   isChild() ? (long) additionalParams.get(PARENT) : jobId;
	}
	
	public boolean isChild() {
		return   additionalParams.containsKey(PARENT);
	}
	
	public Object addAdditionalParam(String key, Object val) {
		return this.additionalParams.put(key, val);
	}
	
	public Object getParamByKey(String key) {
		return this.getAdditionalParams().get(key);
	}
	public enum JobType {
		COPY,SYNC,ARTICLE
	}
}

