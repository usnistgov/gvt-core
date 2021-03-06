package gov.nist.hit.gvt.api;

import java.io.File;
import java.io.IOException;
import java.security.Principal;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import gov.nist.hit.core.domain.ResourceUploadResult;
import gov.nist.hit.core.domain.UserTestCaseGroup;
import gov.nist.hit.core.repo.ConstraintsRepository;
import gov.nist.hit.core.repo.IntegrationProfileRepository;
import gov.nist.hit.core.repo.VocabularyLibraryRepository;
import gov.nist.hit.gvt.domain.GVTSaveInstance;
import gov.nist.hit.gvt.domain.SessionTestCases;
import gov.nist.hit.gvt.domain.UploadStatus;
import gov.nist.hit.gvt.exception.NoUserFoundException;
import gov.nist.hit.gvt.repository.UserTestCaseGroupRepository;
import gov.nist.hit.gvt.service.BundleHandler;
import gov.nist.hit.gvt.service.UserIdService;


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
	

	

	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/cf/groups", method = RequestMethod.GET)
	@ResponseBody
	public SessionTestCases testcases(Principal p) throws NoUserFoundException {
		Long userId = userIdService.getCurrentUserId(p);
		
		if(userId == null)
			throw new NoUserFoundException("User could not be found");

		SessionTestCases stc = new SessionTestCases();
  		stc.setPreloaded(testCaseGroupRepository.findByPreloaded(true));
  		stc.setUser(testCaseGroupRepository.userExclusive(userId));
  		return stc;
	}
	

}
