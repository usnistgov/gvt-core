package gov.nist.hit.gvt.domain;

import java.util.List;

import gov.nist.healthcare.resources.domain.XMLError;
import gov.nist.hit.core.domain.ResourceUploadResult;

public class UploadStatus {
	private ResourceUploadResult status;
	private String message;
	private String debugError;
	
	
	
	public UploadStatus() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	public UploadStatus(ResourceUploadResult status, String message, String debugError) {
		super();
		this.status = status;
		this.message = message;
		this.debugError = debugError;
	}
	
	public UploadStatus(ResourceUploadResult status, String message) {
		super();
		this.status = status;
		this.message = message;
		this.debugError = null;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ResourceUploadResult getStatus() {
		return status;
	}

	public void setStatus(ResourceUploadResult status) {
		this.status = status;
	}

	public String getDebugError() {
		return debugError;
	}

	public void setDebugError(String debugError) {
		this.debugError = debugError;
	}


	
	
	
}
