package gov.nist.hit.gvt.domain;

import gov.nist.hit.core.domain.ResourceUploadResult;

public class UploadStatus {
	private ResourceUploadResult status;
	private String message;
	
	
	public UploadStatus() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public UploadStatus(ResourceUploadResult status, String message) {
		super();
		this.status = status;
		this.message = message;
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
	
	
}
