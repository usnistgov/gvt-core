package gov.nist.hit.gvt.domain;

import java.util.List;

import gov.nist.hit.core.domain.UserTestCaseGroup;

public class SessionTestCases {
	
	private List<UserTestCaseGroup> preloaded;
	private List<UserTestCaseGroup> user;
	

	public List<UserTestCaseGroup> getPreloaded() {
		return preloaded;
	}
	public void setPreloaded(List<UserTestCaseGroup> preloaded) {
		this.preloaded = preloaded;
	}
	public List<UserTestCaseGroup> getUser() {
		return user;
	}
	public void setUser(List<UserTestCaseGroup> user) {
		this.user = user;
	}
	
}
