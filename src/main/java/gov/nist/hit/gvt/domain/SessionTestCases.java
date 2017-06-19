package gov.nist.hit.gvt.domain;

import java.util.List;

import gov.nist.hit.core.domain.CFTestPlan;

public class SessionTestCases {
	
	private List<CFTestPlan> preloaded;
	private List<CFTestPlan> user;
	

	public List<CFTestPlan> getPreloaded() {
		return preloaded;
	}
	public void setPreloaded(List<CFTestPlan> preloaded) {
		this.preloaded = preloaded;
	}
	public List<CFTestPlan> getUser() {
		return user;
	}
	public void setUser(List<CFTestPlan> user) {
		this.user = user;
	}
	
}
