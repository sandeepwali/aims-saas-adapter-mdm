package com.solumesl.aims.saas.adapter.mdm.controller;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.solumesl.aims.saas.adapter.constants.CommonConstants;
import com.solumesl.aims.saas.adapter.constants.SaasConstants;
import com.solumesl.aims.saas.adapter.controller.BaseController;
import com.solumesl.aims.saas.adapter.entity.job.JobStatus;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.mdm.job.MdmResponse;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams;
import com.solumesl.aims.saas.adapter.mdm.model.RequestParams.JobType;
import com.solumesl.aims.saas.adapter.mdm.model.StoreDetail;
import com.solumesl.aims.saas.adapter.mdm.mq.MQProducer;
import com.solumesl.aims.saas.adapter.model.ClientResponse;
import com.solumesl.aims.saas.adapter.types.KVPair;
import com.solumesl.aims.saas.adapter.util.SolumSaasUtil;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 
 * @author baskarmohanasundaram
 *
 */
@OpenAPIDefinition( info = @Info(
		title = "Aims Master Data Management",
		version = "0.1",description = "Article Bulk Push Across Stores"
		),security =@SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(  type = SecuritySchemeType.HTTP,in = SecuritySchemeIn.HEADER, name="bearerAuth", bearerFormat="JWT",scheme = "bearer") 
@RestController
@RequestMapping(path = "/api/v1/")
@Tag(name="Article", description = "Article Bulk Push Across Stores")
public class MdmRequestController extends BaseController {

	@Autowired
	private JobManager jobManager;

	@Autowired
	private MQProducer mqProducer;
	/**
	 * 
	 * @param data
	 * @param company
	 * @param country
	 * @return
	 * @throws Exception
	 */
	@Operation(summary = "Upload Article to all stores by company and region")
	
	@ApiResponses(value = { 
			@ApiResponse(responseCode = "200", description = "The request has been accepted", 
					content = { @Content(mediaType = "application/json",  schema = @Schema(implementation = MdmResponse.class)) }),
			@ApiResponse(responseCode = "400", description = "data is Missing", 
			content = @Content),@ApiResponse(responseCode = "401", description = "Unauthorized", 
			content = { @Content(mediaType = "application/json",  schema = @Schema(implementation = Error.class)) }) }) 
	@PostMapping(value = "/article", consumes = "application/json")
	public ResponseEntity<?> processArticleUpload(@RequestBody @Schema(example = "[{\"articleId\":\"B100001\",\"articleName\":\"OAPDISPENSERLARGE\",\"nfcUrl\":\"http://www.solum.com/p/B100001\",\"eans\":[\"1234567890\"],\"data\":{\"SALE_PRICE\":\"$500\",\"DISCOUNT_PRICE\":\"$100\"}}]") List<KVPair> data, 	@RequestParam(required=true) String company,
			@RequestParam(required=true) String country, @RequestParam(required=false) String region, @RequestParam(required=false) String city ) throws Exception {

		validateRequest(company, country, true , true);
		
		checkUserBelongToCompany(company);
		
		return processRequest(data, generateStoreDetail(company, country, region, city), JobType.COPY);
	}



	
	protected ResponseEntity<?> processRequest(Object data, StoreDetail storeDetail, JobType jobType) {
		if (SolumSaasUtil.isNotEmpty(data)) {
			long jobId = JobManager.generateJobId();
			if(JobType.SYNC.equals(jobType)) {
				data = Lists.newArrayList(data);
			}
			RequestParams  args = new RequestParams((List<KVPair>) data, storeDetail, jobId);
			args.setJobType(jobType);
			jobManager.writeJobStatus(jobId, JobStatus.PRE_ACCEPT);
			mqProducer.sendMaster(args);
			jobManager.writeJobStatus(jobId, JobStatus.ACCEPTED);
			Link link = linkTo(methodOn(JobTrackerController.class).getJobStatus(jobId)).withSelfRel();
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(MdmResponse.builder().jobId(jobId).message("Request Accepted").link(link).build());

		}else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ClientResponse(CommonConstants.FAILURE_CODE, getMessage("nodata")));

		}
	}
	

	
	protected StoreDetail generateStoreDetail(String company, String country, String region, String city) {
		
		StoreDetail storeDetail = new StoreDetail(company, country);
		
		storeDetail.addQuery(SaasConstants.CITY, city);
		storeDetail.addQuery(SaasConstants.REGION, region);
		
		return storeDetail;
	}

}
