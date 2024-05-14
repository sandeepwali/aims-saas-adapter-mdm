package com.solumesl.aims.saas.adapter.mdm.exception.advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.solumesl.aims.saas.adapter.constants.CommonConstants;
import com.solumesl.aims.saas.adapter.model.ClientResponse;
import com.solumesl.aims.saas.adapter.exception.ConstraintViolationException;
import com.solumesl.aims.saas.adapter.exception.InvalidCompany;
/**
 * 
 * @author baskarmohanasundaram
 *
 */
@ControllerAdvice
public class ExceptionAdviceController extends ResponseEntityExceptionHandler  {
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException exc) {
		return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ClientResponse(CommonConstants.FAILURE_CODE, "The file is too large to handle"));
	}
	
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<?> handleMandatoryFieldsException(ConstraintViolationException exc) {
		return ResponseEntity.status(HttpStatus.OK).body(new ClientResponse(CommonConstants.FAILURE_CODE, "Please Provide Mandatory field(s) :" + exc.getMessage()));
	}
	@ExceptionHandler(InvalidCompany.class)
	public ResponseEntity<?> handleMandatoryFieldsException(InvalidCompany exc) {
		return ResponseEntity.status(HttpStatus.OK).body(new ClientResponse(CommonConstants.FAILURE_CODE, "Please check the company information :" + exc.getMessage()));
	}
	@Order(Ordered.LOWEST_PRECEDENCE)
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> GeneralException(Exception exc) {
		return ResponseEntity.status(HttpStatus.OK).body(new ClientResponse(CommonConstants.FAILURE_CODE, "Internal Processing error, Please try later."));
	}
	
	 
}