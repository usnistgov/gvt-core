package gov.nist.hit.gvt.domain;

import java.util.List;

public class TestCaseWrapper {

	
	private List<UploadedProfileModel> testcases;
	private String testcasename;
	private String testcasedescription;
	private String token;
	
	
	public TestCaseWrapper() {
		super();
	}


	public TestCaseWrapper(List<UploadedProfileModel> testcases, String testcasename, String testcasedescription) {
		super();
		this.testcases = testcases;
		this.testcasename = testcasename;
		this.testcasedescription = testcasedescription;
	}
	
	
	
	public TestCaseWrapper(List<UploadedProfileModel> testcases, String testcasename, String testcasedescription, String token) {
		super();
		this.testcases = testcases;
		this.testcasename = testcasename;
		this.testcasedescription = testcasedescription;
		this.token = token;
	}


	public List<UploadedProfileModel> getTestcases() {
		return testcases;
	}
	public void setTestcases(List<UploadedProfileModel> testcases) {
		this.testcases = testcases;
	}
	public String getTestcasename() {
		return testcasename;
	}
	public void setTestcasename(String testcasename) {
		this.testcasename = testcasename;
	}
	public String getTestcasedescription() {
		return testcasedescription;
	}
	public void setTestcasedescription(String testcasedescription) {
		this.testcasedescription = testcasedescription;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	
	
}
