package com.solumesl.aims.saas.adapter.mdm.controller;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;
import com.solumesl.aims.saas.adapter.controller.BaseController;
import com.solumesl.aims.saas.adapter.entity.job.view.View;
import com.solumesl.aims.saas.adapter.job.manager.JobManager;
import com.solumesl.aims.saas.adapter.model.ClientResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 
 * @author baskarmohanasundaram
 *
 */

@RestController
@RequestMapping(path = "/api/v1/")
@Tag(name="Job", description = "Job Status Tracker")
public class JobTrackerController extends BaseController {
	@Autowired
	private JobManager jobManager;
	@Operation(summary = "Get Job Status")

	@ApiResponses(value = { 
			@ApiResponse(responseCode = "200", description = "Success", 
					content = { @Content(mediaType = "application/json",  schema = @Schema(implementation = ClientResponse.class)) }),
			@ApiResponse(responseCode = "400", description = "invalid request", 
			content = @Content),@ApiResponse(responseCode = "401", description = "Unauthorized", 
			content = { @Content(mediaType = "application/json",  schema = @Schema(implementation = Error.class)) }) }) 
	@JsonView(View.Admin.class)
	@GetMapping(value = "/job/{jobId}", produces = "application/json")
	public ResponseEntity<?> getJobStatus(@PathVariable Long jobId)   {
		return Optional
				.ofNullable( jobManager.readJobStatus(jobId) )
				.map( job -> ResponseEntity.ok().body(job) )          //200 OK
				.orElseGet( () -> ResponseEntity.notFound().build() ); 
	}


}