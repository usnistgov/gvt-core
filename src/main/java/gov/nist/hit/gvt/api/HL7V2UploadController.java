/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 */

package gov.nist.hit.gvt.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import gov.nist.healthcare.resources.domain.XMLError;
import gov.nist.hit.core.domain.CFTestPlan;
import gov.nist.hit.core.domain.CFTestStep;
import gov.nist.hit.core.domain.ResourceUploadResult;
import gov.nist.hit.core.domain.TestPlan;
import gov.nist.hit.core.hl7v2.service.HL7V2ProfileParserImpl;
import gov.nist.hit.core.repo.ConstraintsRepository;
import gov.nist.hit.core.repo.IntegrationProfileRepository;
import gov.nist.hit.core.repo.VocabularyLibraryRepository;
import gov.nist.hit.core.service.ProfileParser;
import gov.nist.hit.core.service.exception.MessageUploadException;
import gov.nist.hit.gvt.domain.GVTSaveInstance;
import gov.nist.hit.gvt.domain.LongResult;
import gov.nist.hit.gvt.domain.TestCaseWrapper;
import gov.nist.hit.gvt.domain.Token;
import gov.nist.hit.gvt.domain.UploadStatus;
import gov.nist.hit.gvt.domain.UploadedProfileModel;
import gov.nist.hit.gvt.exception.NoUserFoundException;
import gov.nist.hit.gvt.exception.NotValidToken;
import gov.nist.hit.gvt.repository.UserTestCaseGroupRepository;
import gov.nist.hit.gvt.service.BundleHandler;
import gov.nist.hit.gvt.service.FileValidationHandler;
import gov.nist.hit.gvt.service.PackagingHandler;
import gov.nist.hit.gvt.service.UserIdService;
import io.swagger.annotations.Api;

/**
 * @author Nicolas Crouzier (NIST)
 * 
 */

@RequestMapping("/upload")
@Api(hidden = true)
@Controller
public class HL7V2UploadController {

	static final Logger logger = LoggerFactory.getLogger(HL7V2UploadController.class);

	private final String tmpDir = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() +"/gvt";
	
	ProfileParser parser = new HL7V2ProfileParserImpl();

	
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
	private PackagingHandler packagingHandler;

	@Autowired
	private FileValidationHandler fileValidationHandler;


	
	
	
	/**
	 * Upload a single XML profile file and may returns errors
	 * @param request Client request
	 * @param part Profile XML file
	 * @param token Token used for saving file
	 * @param p Principal
	 * @return A list of profiles or a list of errors
	 * @throws MessageUploadException
	 */	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/uploadProfile", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> uploadProfile(ServletRequest request, @RequestPart("file") MultipartFile part,@RequestParam("token") String token, Principal p) throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("text/xml"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.xml' ");

			String userName = userIdService.getCurrentUserName(p);
			if (userName == null)
				throw new NoUserFoundException("User could not be found");

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			org.apache.commons.io.IOUtils.copy(part.getInputStream(), baos);
			byte[] bytes = baos.toByteArray();
			
			String content = IOUtils.toString(new ByteArrayInputStream(bytes));
			List<XMLError> errors = fileValidationHandler.validateProfile(content,new ByteArrayInputStream(bytes));

			

			if (errors.size() > 0) {
				resultMap.put("success", false);
				resultMap.put("errors", errors);
				logger.info("Uploaded profile file with errors " + part.getName());
			} else {
				List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(content);
				resultMap.put("success", true);
				resultMap.put("profiles", list);

				File profileFile = new File(tmpDir+ "/" + userName  + "/" + token + "/Profile.xml");
				FileUtils.writeStringToFile(profileFile, content);
				logger.info("Uploaded valid profile file " + part.getName());
			}

			return resultMap;
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the profile file sent");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (MessageUploadException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the profile file sent");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (Exception e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the profile file sent");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		}
	}

	/**
	 * Upload a single XML value set file and may returns errors
	 * @param request Client request
	 * @param part Value Set XML file
	 * @param token Token used for saving file
	 * @param p Principal
	 * @return  May return a list of errors
	 * @throws MessageUploadException
	 */
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/uploadVS", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> uploadVS(ServletRequest request, @RequestPart("file") MultipartFile part,@RequestParam("token") String token, Principal p) throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("text/xml"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.xml' ");

			String userName = userIdService.getCurrentUserName(p);
			if (userName == null)
				throw new NoUserFoundException("User could not be found");

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			org.apache.commons.io.IOUtils.copy(part.getInputStream(), baos);
			byte[] bytes = baos.toByteArray();
			String content = IOUtils.toString(new ByteArrayInputStream(bytes));
			List<XMLError> errors = fileValidationHandler.validateVocabulary(content,new ByteArrayInputStream(bytes));

			

			if (errors.size() > 0) {
				resultMap.put("success", false);
				resultMap.put("errors", errors);
				logger.info("Uploaded value set file with errors " + part.getName());
			} else {
				resultMap.put("success", true);

				File vsFile = new File(tmpDir+ "/" + userName  + "/" + token + "/ValueSets.xml");
				FileUtils.writeStringToFile(vsFile, content);
				logger.info("Uploaded value set file " + part.getName());
			}
			return resultMap;
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the valueset file sent");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (MessageUploadException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the valueset file sent");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (Exception e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the valueset file sent");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		}
	}

	/**
	 * Upload a single XML constraints file and may returns errors
	 * @param request Client request
	 * @param part Constraints XML file
	 * @param token Token used for saving file
	 * @param p Principal
	 * @return  May return a list of errors
	 * @throws MessageUploadException
	 */
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/uploadContraints", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> uploadContraints(ServletRequest request, @RequestPart("file") MultipartFile part,@RequestParam("token") String token,Principal p) throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("text/xml"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.xml' ");

			String userName = userIdService.getCurrentUserName(p);
			if (userName == null)
				throw new NoUserFoundException("User could not be found");

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			org.apache.commons.io.IOUtils.copy(part.getInputStream(), baos);
			byte[] bytes = baos.toByteArray();
			String content = IOUtils.toString(new ByteArrayInputStream(bytes));
			List<XMLError> errors = fileValidationHandler.validateConstraints(content,new ByteArrayInputStream(bytes));

			

			if (errors.size() > 0) {
				resultMap.put("success", false);
				resultMap.put("errors", errors);
				logger.info("Uploaded constraints file with errors " + part.getName());
			} else {
				resultMap.put("success", true);

				File constraintFile = new File(tmpDir+ "/" + userName + "/" + token + "/Constraints.xml");
				FileUtils.writeStringToFile(constraintFile, content);
				logger.info("Uploaded constraints file " + part.getName());
			}
			return resultMap;
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the constraints file sent");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (MessageUploadException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the constraints file sent");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (Exception e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the constraints file sent");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		}
	}
	
	/**
	 * Uploads zip file and stores it in a temporary directory
	 * @param request Client request
	 * @param part Zip file
	 * @param p Principal
	 * @return a token or some errors
	 * @throws MessageUploadException
	 */
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/uploadZip", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> uploadZip(ServletRequest request, @RequestPart("file") MultipartFile part, Principal p)
			throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("application/zip"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.zip' ");

			String userName = userIdService.getCurrentUserName(p);
			if (userName == null)
				throw new NoUserFoundException("User could not be found");



			String token =  UUID.randomUUID().toString();
			String directory = bundleHandler.unzip(part.getBytes(),tmpDir+ "/" + userName + "/" + token);
			Map<String, List<XMLError>> errorMap = fileValidationHandler.unbundleAndValidate(directory);
			

			if (errorMap.get("profileErrors").size() > 0 || errorMap.get("constraintsErrors").size() > 0
					|| errorMap.get("vsErrors").size() > 0) {
				resultMap.put("success", false);
				resultMap.put("profileErrors", errorMap.get("profileErrors"));
				resultMap.put("constraintsErrors", errorMap.get("constraintsErrors"));
				resultMap.put("vsErrors", errorMap.get("vsErrors"));
				FileUtils.deleteDirectory(new File(directory));
				logger.info("Uploaded profile file with errors " + part.getName());
			} else {
				resultMap.put("success", true);
			    resultMap.put("token", token);
				
				logger.info("Uploaded valid zip File file " + part.getName());
			}

			
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the zip file sent");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (MessageUploadException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the zip file sent");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (Exception e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not upload the zip file sent");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		}
		return resultMap;
	}
	
	
	/**
	 * Retrieves a list of profiles from previously uploaded files 
	 * @param request Client request
	 * @param token token from uploaded files
	 * @param p Principal
	 * @return A list of profiles
	 * @throws MessageUploadException
	 */
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "uploadedProfiles", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> uploadedProfiles(ServletRequest request,@RequestBody Token token, Principal p)
			throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {

			String userName = userIdService.getCurrentUserName(p);
			if (userName == null)
				throw new NoUserFoundException("User could not be found");

			String directory = tmpDir+ "/" + userName + "/" + token.getToken();
			if (!new File(directory).exists())
				throw new NotValidToken("The provided token is not valid for this account.");
			
			
			String profileContent = bundleHandler.getProfileContentFromZipDirectory(directory);
			
			if (profileContent == null)
				throw new MessageUploadException("Could not retrieve the profile list");

			List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(profileContent);
			resultMap.put("success", true);
			resultMap.put("profiles", list);	

			logger.info("retrieved profile info from upload");

			
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not retrieve the uploaded profiles");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (NotValidToken e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not retrieve the uploaded profiles");
			resultMap.put("debugError", ExceptionUtils.getMessage(e));
			return resultMap;
		} catch (Exception e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured. The tool could not retrieve the uploaded profiles");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		}
		return resultMap;
	}
	
	/**
	 * Add selected profiles to the database
	 * @param request Client request
	 * @param wrapper Selected profile information
	 * @param p Principal
	 * @return UploadStatus
	 */
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/addProfiles", method = RequestMethod.POST)
	@ResponseBody
	public UploadStatus addProfiles(ServletRequest request, @RequestBody TestCaseWrapper wrapper, Principal p) {
		try {
			String userName = userIdService.getCurrentUserName(p);
			if (userName == null)
				throw new NoUserFoundException("User could not be found");
				
			// Create needed files
			JSONObject testCaseJson = new JSONObject();
			testCaseJson.put("name", wrapper.getTestcasename());
			testCaseJson.put("description", wrapper.getTestcasedescription());
			testCaseJson.put("profile", "Profile.xml");
			testCaseJson.put("constraints", "Constraints.xml");
			testCaseJson.put("vs", "ValueSets.xml");
							
			JSONArray testSteps = new JSONArray();
			for (UploadedProfileModel upm : wrapper.getTestcases()) {
				JSONObject ts = new JSONObject();
				ts.put("name", upm.getName());
				ts.put("messageId", upm.getId());
				ts.put("description", upm.getDescription());
				testSteps.put(ts);
			}
			testCaseJson.put("testCases", testSteps);
			
			File jsonFile = new File(tmpDir+ "/" + userName + "/"  + wrapper.getToken() + "/TestCases.json");
			File profileFile = new File(tmpDir+ "/" + userName + "/"  + wrapper.getToken() + "/Profile.xml");
			File constraintsFile = new File(tmpDir+ "/" + userName + "/"  + wrapper.getToken() + "/Constraints.xml");
			File vsFile = new File(tmpDir+ "/" + userName + "/"  + wrapper.getToken() + "/ValueSets.xml");
			
			if (constraintsFile != null) {
				packagingHandler.changeConstraintId(constraintsFile);
			}
			if (vsFile != null) {
				packagingHandler.changeVsId(vsFile);
			}				

			InputStream targetStream = new FileInputStream(profileFile);
			String content = IOUtils.toString(targetStream);
			String cleanedContent = packagingHandler.removeUnusedAndDuplicateMessages(content, wrapper.getTestcases());
			FileUtils.writeStringToFile(profileFile, cleanedContent);
			packagingHandler.changeProfileId(profileFile);

			FileUtils.writeStringToFile(jsonFile, testCaseJson.toString());

			// Use files to save to database
			GVTSaveInstance si;
			if (wrapper.getGroupId() == null){
				si = bundleHandler.createGVTSaveInstance(tmpDir+ "/" + userName + "/"  + wrapper.getToken());
				List<CFTestPlan> list = testCaseGroupRepository.userExclusive(userName);
				si.tcg.setPosition(list.size()+1);
			}else{
				CFTestPlan tp = testCaseGroupRepository.findOne(wrapper.getGroupId());
				if (tp == null){
					throw new Exception("Profile Group could not be found");
				}
				si = bundleHandler.createGVTSaveInstance(tmpDir+ "/" + userName + "/"  + wrapper.getToken(),tp);
			}
			
			
			ipRepository.save(si.ip);
			csRepository.save(si.ct);
			vsRepository.save(si.vs);
			si.tcg.setAuthorUsername(userName);
			
			
			testCaseGroupRepository.saveAndFlush(si.tcg);
			FileUtils.deleteDirectory(new File(tmpDir+ "/" + userName + "/"  + wrapper.getToken()));
			return new UploadStatus(ResourceUploadResult.SUCCESS, "Test Cases Group has been added",si.tcg.getId());

		} catch (IOException e) {
			return new UploadStatus(ResourceUploadResult.FAILURE, "IO Error could not read files", ExceptionUtils.getStackTrace(e));
		} catch (NoUserFoundException e) {
			return new UploadStatus(ResourceUploadResult.FAILURE, "User could not be found", ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			return new UploadStatus(ResourceUploadResult.FAILURE,  "An error occured while adding profiles", ExceptionUtils.getStackTrace(e));
		}

	}

	
	
	/**
	 * Clear files in tmp directory
	 * @param request Client request
	 * @param token files' token
	 * @param p Principal
	 * @return True/False as success indicator
	 */
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/clearFiles", method = RequestMethod.POST)
	@ResponseBody
	public boolean clearFiles(ServletRequest request, @RequestBody Token token, Principal p) {

		try {
			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException("User could not be found");
			
			FileUtils.deleteDirectory(new File(tmpDir+ "/" + userId + "/"  + token.getToken()));
			return true;
		} catch (NoUserFoundException | IOException e) {
			return false;
		}
	}
	
	
	
	/**
	 * Delete a profile from the database
	 * @param request Client request
	 * @param lr Profile id
	 * @param p Principal
	 * @return True/False as success indicator
	 * @throws NoUserFoundException
	 */
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/deleteProfile", method = RequestMethod.POST)
	@ResponseBody
	@Transactional(value = "transactionManager")
	public boolean deleteProfile(ServletRequest request, @RequestBody LongResult lr, Principal p) throws NoUserFoundException{
		boolean res = true;
		String userName = userIdService.getCurrentUserName(p);
		
		if(userName == null){
			throw new NoUserFoundException("User could not be found");
		}
				
		List<CFTestPlan> list = testCaseGroupRepository.userExclusive(userName);
		boolean found =false;
		for (CFTestPlan utg : list){
			
			for (Iterator<CFTestStep> iterator =  utg.getTestCases().iterator(); iterator.hasNext();) {
				CFTestStep ucf = iterator.next();
				if (ucf.getId().equals(lr.getId())){
					 iterator.remove();
					 found = true;
				}
			}
			if (found){
				if (utg.getTestCases().size() == 0 ){
					testCaseGroupRepository.delete(utg);
					res = true;
				}else{
					testCaseGroupRepository.save(utg);
					res = false;
				}
				break;
			}
			
		}
		
		//return true if testPlan is also deleted, false otherwise 
		return res;
	}
	
	
	

}
