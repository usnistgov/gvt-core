package gov.nist.hit.gvt.api;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import gov.nist.auth.hit.core.domain.Account;
import gov.nist.hit.core.api.SessionContext;
import gov.nist.hit.core.domain.CFTestPlan;
import gov.nist.hit.core.domain.TestScope;
import gov.nist.hit.core.domain.TestingStage;
import gov.nist.hit.core.repo.ConstraintsRepository;
import gov.nist.hit.core.repo.IntegrationProfileRepository;
import gov.nist.hit.core.repo.VocabularyLibraryRepository;
import gov.nist.hit.core.service.AccountService;
import gov.nist.hit.core.service.CFTestPlanService;
import gov.nist.hit.core.service.Streamer;
import gov.nist.hit.gvt.domain.SessionTestCases;
import gov.nist.hit.gvt.exception.NoUserFoundException;
import gov.nist.hit.gvt.repository.UserTestCaseGroupRepository;
import gov.nist.hit.gvt.service.BundleHandler;
import gov.nist.hit.gvt.service.UserIdService;
import io.swagger.annotations.ApiParam;


@Controller
public class HL7V2CFTestCaseController {

	@Autowired
	private UserTestCaseGroupRepository testCaseGroupRepository;
	
	@Autowired
	private IntegrationProfileRepository ipRepository;
	
	@Autowired
	private ConstraintsRepository csRepository;
	
	@Autowired
	private VocabularyLibraryRepository vsRepository;
	
	@Autowired
	private UserIdService userIdService;

	@Autowired
	private BundleHandler bundleHandler;
	
	@Autowired
	private CFTestPlanService testPlanService;

	@Autowired
	private AccountService userService;
	
	@Autowired
	private Streamer streamer;

	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/cf/groups", method = RequestMethod.GET)
	@ResponseBody
	public SessionTestCases testcases(Principal p) throws NoUserFoundException {
		String userName = userIdService.getCurrentUserName(p);
		
		if(userName == null)
			throw new NoUserFoundException("User could not be found");

		SessionTestCases stc = new SessionTestCases();
  		stc.setPreloaded(testCaseGroupRepository.findByPreloaded(true));
  		stc.setUser(testCaseGroupRepository.userExclusive(userName));
  		return stc;
    }
	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/gvt/profileGroups", method = RequestMethod.GET, produces = "application/json")
	public void getGroupsByScope(
			@ApiParam(value = "the scope of the test plans", required = false) @RequestParam(required = true) TestScope scope,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<CFTestPlan> results = null;
		scope = scope == null ? TestScope.GLOBAL : scope;
		String username = null;
		Long userId = SessionContext.getCurrentUserId(request.getSession(false));
		if (userId != null) {
			Account account = userService.findOne(userId);
			if(account != null){
				username = account.getUsername();
			}
		}
		results = testPlanService.findShortAllByScopeAndUsername(scope, username);
		streamer.stream2(response.getOutputStream(), results);
	}
	

}
