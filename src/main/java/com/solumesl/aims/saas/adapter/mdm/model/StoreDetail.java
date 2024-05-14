package com.solumesl.aims.saas.adapter.mdm.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.solumesl.aims.saas.adapter.util.SolumSaasUtil;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StoreDetail implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String company;
	public StoreDetail(String company, String country ) {
		super();
		this.company = company;
		this.country = country;
	}
	private String country;
	private String store;
	
	private Map<String, String> queryParams = new HashMap<String, String>();
	
	public String addQuery(String key, String value) {
		if(SolumSaasUtil.isNotEmpty(key) && SolumSaasUtil.isNotEmpty(value)) {
			return queryParams.putIfAbsent(key, value);
		}
		return value;
		
	}
}
