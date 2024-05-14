package com.solumesl.aims.saas.adapter.mdm.job;

import org.springframework.hateoas.Link;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class MdmResponse {

	private String message;
	private Long jobId;
	private Link link;
}
