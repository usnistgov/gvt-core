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

@RequestMapping("/gvt")
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
	@RequestMapping(value = "/groups", method = RequestMethod.GET)
	@ResponseBody
	public SessionTestCases testcases(Principal p) throws NoUserFoundException {
		Long userId = userIdService.getCurrentUserId(p);
		
		if(userId == null)
			throw new NoUserFoundException();

		SessionTestCases stc = new SessionTestCases();
  		stc.setPreloaded(testCaseGroupRepository.findByPreloaded(true));
  		stc.setUser(testCaseGroupRepository.userExclusive(userId));
  		return stc;
	}
	
	
//	@PreAuthorize("hasRole('tester')")
//	@RequestMapping(value = "/bundle/upload", method = RequestMethod.POST, consumes = { "multipart/form-data" })
//	@ResponseBody
//	public UploadStatus bundle(@RequestPart("file") MultipartFile file,Principal p) {
//		try {
//			if(!file.isEmpty()){
//				Long userId = userIdService.getCurrentUserId(p);
//				if(userId == null)
//					throw new NoUserFoundException();
//				
//				String directory = bundleHandler.unzip(file.getBytes());
//				System.out.println("UNBUNDLING");
//				GVTSaveInstance si = bundleHandler.unbundle(directory);
//				FileUtils.deleteDirectory(new File(directory));
//				ipRepository.save(si.ip);
//				csRepository.save(si.ct);
//				vsRepository.save(si.vs);
//				si.tcg.setUserId(userId);
//				System.out.println("SAVING");
//				testCaseGroupRepository.saveAndFlush(si.tcg);
//				System.out.println("SAVED");
//				return new UploadStatus(ResourceUploadResult.SUCCESS,"Test Cases Group has been added");
//			}
//			else {
//				return new UploadStatus(ResourceUploadResult.FAILURE,"Submitted bundle is empty");
//			}
//		}
//		catch(IOException e){
//			return new UploadStatus(ResourceUploadResult.FAILURE,"IO Error could not read bundle");
//		} catch (NoUserFoundException e) {
//			return new UploadStatus(ResourceUploadResult.FAILURE,"No User Found");
//		} catch (Exception e) {
//			e.printStackTrace();
//			return new UploadStatus(ResourceUploadResult.FAILURE,"Could not read bundle, "
//					+ "please check that format is correct");
//		}
//	}
	
//	//@PreAuthorize("hasRole('admin')")
//	@PreAuthorize("hasRole('tester')")
//	@RequestMapping(value = "/preload/{id}", method = RequestMethod.GET)
//	@ResponseBody
//	public UploadStatus preload(@PathVariable Long id,Principal principal) throws NoUserFoundException {
//		UserTestCaseGroup find = testCaseGroupRepository.findOne(id);
//		if(find != null){
//			find.setPreloaded(true);
//			testCaseGroupRepository.save(find);
//			return new UploadStatus(ResourceUploadResult.SUCCESS,"TestCase Group "+id+" is now preloaded");
//		}
//		else {
//			return new UploadStatus(ResourceUploadResult.FAILURE,"TestCase Group "+id+" not found");
//		}
//	}
	
}
