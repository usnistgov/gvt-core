package gov.nist.hit.gvt.domain;

import java.util.List;

public class SessionTestCases {
	
	private List<GVTTestCaseGroup> preloaded;
	private List<GVTTestCaseGroup> user;
	

	public List<GVTTestCaseGroup> getPreloaded() {
		return preloaded;
	}
	public void setPreloaded(List<GVTTestCaseGroup> preloaded) {
		this.preloaded = preloaded;
	}
	public List<GVTTestCaseGroup> getUser() {
		return user;
	}
	public void setUser(List<GVTTestCaseGroup> user) {
		this.user = user;
	}
	
}
