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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.io.Files;

import gov.nist.auth.hit.core.domain.util.UserUtil;
import gov.nist.healthcare.resources.domain.XMLError;
import gov.nist.hit.core.domain.ResourceUploadResult;
import gov.nist.hit.core.domain.UserCFTestInstance;
import gov.nist.hit.core.domain.UserTestCaseGroup;
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
import gov.nist.hit.gvt.repository.UserCFTestInstanceRepository;
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
@RequestMapping("/gvt")
@Api(hidden = true)
@Controller
public class HL7V2UploadController {

	static final Logger logger = LoggerFactory.getLogger(HL7V2UploadController.class);

	ProfileParser parser = new HL7V2ProfileParserImpl();

	@Autowired
	private UserCFTestInstanceRepository userCFTestInstanceRepository;
	
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

//	private String zipDirectoryIGAMT = null;
//	private final List<File> profileFileListIGAMT = new ArrayList<File>();
//	private final List<File> valueSetFileListIGAMT = new ArrayList<File>();
//	private final List<File> constraintFileListIGAMT = new ArrayList<File>();
	
	private final List<File> profileFileList = new ArrayList<File>();
	private final List<File> valueSetFileList = new ArrayList<File>();
	private final List<File> constraintFileList = new ArrayList<File>();

	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/upload/uploadzip", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> uploadzip(ServletRequest request, @RequestPart("file") MultipartFile part, Principal p)
			throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("application/zip"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.zip' ");

			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();

//			InputStream in = part.getInputStream();
//			String content = IOUtils.toString(in);

			String directory = bundleHandler.unzip(part.getBytes());
			Map<String, List<XMLError>> errorMap = fileValidationHandler.unbundleAndValidate(directory);
			String profileContent = bundleHandler.getProfileContentFromZipDirectory(directory);
			

			if (errorMap.get("profileErrors").size() > 0 || errorMap.get("constraintsErrors").size() > 0
					|| errorMap.get("vsErrors").size() > 0) {
				resultMap.put("success", false);
				resultMap.put("profileErrors", errorMap.get("profileErrors"));
				resultMap.put("constraintsErrors", errorMap.get("constraintsErrors"));
				resultMap.put("vsErrors", errorMap.get("vsErrors"));
				FileUtils.deleteDirectory(new File(directory));
				logger.info("Uploaded profile file with errors " + part.getName());
			} else {
				List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(profileContent);
				resultMap.put("success", true);
				resultMap.put("profiles", list);
				
				File profileFile = new File(request.getServletContext().getRealPath("tmp/" + userId + "/Profile.xml"));
				FileUtils.writeStringToFile(profileFile, bundleHandler.getProfileContentFromZipDirectory(directory));
				profileFileList.add(profileFile);
				
				File valueSetFile = new File(request.getServletContext().getRealPath("tmp/" + userId + "/ValueSets.xml"));
				FileUtils.writeStringToFile(valueSetFile, bundleHandler.getValueSetContentFromZipDirectory(directory));
				valueSetFileList.add(valueSetFile);
				
				File constraintFile = new File(request.getServletContext().getRealPath("tmp/" + userId + "/Constraints.xml"));
				FileUtils.writeStringToFile(constraintFile, bundleHandler.getConstraintContentFromZipDirectory(directory));
				constraintFileList.add(constraintFile);
				
				FileUtils.deleteDirectory(new File(directory));

				logger.info("Uploaded valid zip File file " + part.getName());
			}

			
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "User not found error");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		} catch (Exception e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		}
		return resultMap;
	}
	
	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/upload/igamtuploadzip", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> igamtuploadzip(ServletRequest request, @RequestPart("file") MultipartFile part, Principal p)
			throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("application/zip"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.zip' ");

			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();


			String directory = bundleHandler.unzip(part.getBytes());
//			zipDirectoryIGAMT = directory;
			Map<String, List<XMLError>> errorMap = fileValidationHandler.unbundleAndValidate(directory);
			String profileContent = bundleHandler.getProfileContentFromZipDirectory(directory);
			

			if (errorMap.get("profileErrors").size() > 0 || errorMap.get("constraintsErrors").size() > 0
					|| errorMap.get("vsErrors").size() > 0) {
				resultMap.put("success", false);
				resultMap.put("profileErrors", errorMap.get("profileErrors"));
				resultMap.put("constraintsErrors", errorMap.get("constraintsErrors"));
				resultMap.put("vsErrors", errorMap.get("vsErrors"));
				logger.info("Uploaded profile file with errors " + part.getName());
			} else {
				List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(profileContent);
				resultMap.put("success", true);
				resultMap.put("profiles", list);
			
				String token =  UUID.randomUUID().toString();
			    resultMap.put("token", token);
				
				File profileFile = new File(request.getServletContext().getRealPath("tmp/" + token + "/" + userId + "/Profile.xml"));
				FileUtils.writeStringToFile(profileFile, bundleHandler.getProfileContentFromZipDirectory(directory));
//				profileFileListIGAMT.add(profileFile);
				
				File valueSetFile = new File(request.getServletContext().getRealPath("tmp/" + token + "/" + userId + "/ValueSets.xml"));
				FileUtils.writeStringToFile(valueSetFile, bundleHandler.getValueSetContentFromZipDirectory(directory));
//				valueSetFileListIGAMT.add(valueSetFile);
				
				File constraintFile = new File(request.getServletContext().getRealPath("tmp/" + token + "/" + userId + "/Constraints.xml"));
				FileUtils.writeStringToFile(constraintFile, bundleHandler.getConstraintContentFromZipDirectory(directory));
//				constraintFileListIGAMT.add(constraintFile);
				

				logger.info("Uploaded valid zip File file " + part.getName());
			}

			
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "User not found error");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		} catch (Exception e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		}
		return resultMap;
	}
	
	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/upload/igamtuploadzipprofiles", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> igamtuploadzipprofiles(ServletRequest request,@RequestBody Token token, Principal p)
			throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {

			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();

			String zipDirectoryIGAMT = request.getServletContext().getRealPath("tmp/" + token.getToken() + "/" + userId);
			if (zipDirectoryIGAMT == null)
				throw new MessageUploadException("Zip files not found for this user");
			
			String profileContent = bundleHandler.getProfileContentFromZipDirectory(zipDirectoryIGAMT);
			

			List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(profileContent);
			resultMap.put("success", true);
			resultMap.put("profiles", list);	
//			FileUtils.deleteDirectory(new File(zipDirectoryIGAMT));

			logger.info("retrieved profile info from IGAMT upload");

			
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "User not found error");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		} catch (Exception e) {
			resultMap.put("success", false);
			resultMap.put("message", "An error occured");
			resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
			return resultMap;
		}
		return resultMap;
	}
	
	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/upload/uploadprofile", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> uploadprofile(ServletRequest request, @RequestPart("file") MultipartFile part,
			Principal p) throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("text/xml"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.xml' ");

			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();

			InputStream in = part.getInputStream();
			String content = IOUtils.toString(in);
			List<XMLError> errors = fileValidationHandler.validateProfile(content);

			if (errors.size() > 0) {
				resultMap.put("success", false);
				resultMap.put("errors", errors);
				logger.info("Uploaded profile file with errors " + part.getName());
			} else {
				List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(content);
				resultMap.put("success", true);
				resultMap.put("profiles", list);

				File profileFile = new File(request.getServletContext().getRealPath("tmp/" + userId + "/Profile.xml"));
				FileUtils.writeStringToFile(profileFile, content);
				profileFileList.add(profileFile);

				logger.info("Uploaded valid profile file " + part.getName());
			}

			return resultMap;
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new MessageUploadException(e);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new MessageUploadException(e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MessageUploadException(e);
		} catch (NoUserFoundException e) {
			resultMap.put("success", false);
			resultMap.put("message", "No User Found");
			return resultMap;
		} catch (Exception e) {
			e.printStackTrace();
			throw new MessageUploadException(e);
		}
	}

	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/upload/uploadvs", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> uploadvs(ServletRequest request, @RequestPart("file") MultipartFile part, Principal p)
			throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("text/xml"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.xml' ");

			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();

			InputStream in = part.getInputStream();
			String content = IOUtils.toString(in);
			List<XMLError> errors = fileValidationHandler.validateVocabulary(content);

			if (errors.size() > 0) {
				resultMap.put("success", false);
				resultMap.put("errors", errors);
				logger.info("Uploaded value set file with errors " + part.getName());
			} else {
				resultMap.put("success", true);

				File vsFile = new File(request.getServletContext().getRealPath("tmp/" + userId + "/ValueSets.xml"));
				FileUtils.writeStringToFile(vsFile, content);
				valueSetFileList.add(vsFile);
				logger.info("Uploaded value set file " + part.getName());
			}
			return resultMap;
		} catch (RuntimeException e) {
			throw new MessageUploadException(e);
		} catch (Exception e) {
			throw new MessageUploadException(e);
		}
	}

	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/upload/uploadcontraints", method = RequestMethod.POST, consumes = { "multipart/form-data" })
	@ResponseBody
	public Map<String, Object> uploadcontraints(ServletRequest request, @RequestPart("file") MultipartFile part,
			Principal p) throws MessageUploadException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if (!part.getContentType().equalsIgnoreCase("text/xml"))
				throw new MessageUploadException("Unsupported content type. Supported content types are: '.xml' ");

			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();

			InputStream in = part.getInputStream();
			String content = IOUtils.toString(in);
			List<XMLError> errors = fileValidationHandler.validateConstraints(content);

			if (errors.size() > 0) {
				resultMap.put("success", false);
				resultMap.put("errors", errors);
				logger.info("Uploaded constraints file with errors " + part.getName());
			} else {
				resultMap.put("success", true);

				File constraintFile = new File(
						request.getServletContext().getRealPath("tmp/" + userId + "/Constraints.xml"));
				FileUtils.writeStringToFile(constraintFile, content);
				constraintFileList.add(constraintFile);
				logger.info("Uploaded constraints file " + part.getName());
			}
			return resultMap;
		} catch (RuntimeException e) {
			throw new MessageUploadException(e);
		} catch (Exception e) {
			throw new MessageUploadException(e);
		}
	}
	
	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/addtestcases", method = RequestMethod.POST)
	@ResponseBody
	public UploadStatus addtestcases(ServletRequest request, @RequestBody TestCaseWrapper wrapper, Principal p) {
		try {
			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();
				File zip;
				
				// dealing with individual files
				JSONObject testCaseJson = new JSONObject();
				testCaseJson.put("name", wrapper.getTestcasename());
				testCaseJson.put("description", wrapper.getTestcasedescription());
				testCaseJson.put("profile", profileFileList.get(0).getName());
				if (!constraintFileList.isEmpty()) {
					testCaseJson.put("constraints", constraintFileList.get(0).getName());
				}
				if (!valueSetFileList.isEmpty()) {
					testCaseJson.put("vs", valueSetFileList.get(0).getName());
				}
				JSONArray testSteps = new JSONArray();
				for (UploadedProfileModel upm : wrapper.getTestcases()) {
					JSONObject ts = new JSONObject();
					ts.put("name", upm.getName());
					ts.put("messageId", upm.getId());
					ts.put("description", upm.getDescription());
					testSteps.put(ts);
				}
				testCaseJson.put("testCases", testSteps);
				File jsonFile = new File(request.getServletContext().getRealPath("tmp/" + userId + "/TestCases.json"));

				List<File> files = new ArrayList<File>();

				if (!constraintFileList.isEmpty()) {
					packagingHandler.changeConstraintId(constraintFileList.get(0));
					files.add(constraintFileList.get(0));
				}
				if (!valueSetFileList.isEmpty()) {
					packagingHandler.changeVsId(valueSetFileList.get(0));
					files.add(valueSetFileList.get(0));
				}

				InputStream targetStream = new FileInputStream(profileFileList.get(0));
				String content = IOUtils.toString(targetStream);
				String cleanedContent = packagingHandler.removeUnusedAndDuplicateMessages(content, wrapper.getTestcases());
				FileUtils.writeStringToFile(profileFileList.get(0), cleanedContent);
				packagingHandler.changeProfileId(profileFileList.get(0));
				files.add(profileFileList.get(0));

				FileUtils.writeStringToFile(jsonFile, testCaseJson.toString());
				files.add(jsonFile);
				zip = packagingHandler.zip(files,
				request.getServletContext().getRealPath("tmp/" + userId + "/testcase.zip"));
//			}

			if (zip != null) {

				String directory2 = bundleHandler.unzip(Files.toByteArray(zip));
				GVTSaveInstance si = bundleHandler.unbundle(directory2);
				FileUtils.deleteDirectory(new File(directory2));
				ipRepository.save(si.ip);
				csRepository.save(si.ct);
				vsRepository.save(si.vs);
				si.tcg.setUserId(userId);
				testCaseGroupRepository.saveAndFlush(si.tcg);

				return new UploadStatus(ResourceUploadResult.SUCCESS, "Test Cases Group has been added");
			} else {
				return new UploadStatus(ResourceUploadResult.FAILURE, "Submitted bundle is empty");
			}
		} catch (IOException e) {
			return new UploadStatus(ResourceUploadResult.FAILURE, "IO Error could not read bundle");
		} catch (NoUserFoundException e) {
			return new UploadStatus(ResourceUploadResult.FAILURE, "No User Found");
		} catch (Exception e) {
			e.printStackTrace();
			return new UploadStatus(ResourceUploadResult.FAILURE,
					"Could not read bundle, " + "please check that format is correct");
		}

	}

	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/cleartestcases", method = RequestMethod.POST)
	@ResponseBody
	public boolean cleartestcases(ServletRequest request, Principal p) {

		try {
			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();
			FileUtils.deleteDirectory(new File(request.getServletContext().getRealPath("tmp/" + userId)));
			profileFileList.clear();
			valueSetFileList.clear();
			constraintFileList.clear();
	
			return true;
		} catch (NoUserFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}
	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/igamtcleartestcases", method = RequestMethod.POST)
	@ResponseBody
	public boolean igamtcleartestcases(ServletRequest request, @RequestBody Token token, Principal p) {

		try {
			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();
			FileUtils.deleteDirectory(new File(request.getServletContext().getRealPath("tmp/" + token.getToken() + "/" + userId)));
	
			return true;
		} catch (NoUserFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}
	
	
	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/igamtaddtestcases", method = RequestMethod.POST)
	@ResponseBody
	public UploadStatus igamtaddtestcases(ServletRequest request, @RequestBody TestCaseWrapper wrapper, Principal p) {
		try {
			Long userId = userIdService.getCurrentUserId(p);
			if (userId == null)
				throw new NoUserFoundException();
				File zip;
				
				// dealing with individual files
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
				File jsonFile = new File(request.getServletContext().getRealPath("tmp/" + wrapper.getToken() + "/"  + userId + "/TestCases.json"));
				File profileFile = new File(request.getServletContext().getRealPath("tmp/" + wrapper.getToken() + "/"  + userId + "/Profile.xml"));
				File constraintsFile = new File(request.getServletContext().getRealPath("tmp/" + wrapper.getToken() + "/"  + userId + "/Constraints.xml"));
				File vsFile = new File(request.getServletContext().getRealPath("tmp/" + wrapper.getToken() + "/"  + userId + "/ValueSets.xml"));
				
				List<File> files = new ArrayList<File>();

				if (constraintsFile != null) {
					packagingHandler.changeConstraintId(constraintsFile);
					files.add(constraintsFile);
				}
				if (vsFile != null) {
					packagingHandler.changeVsId(vsFile);
					files.add(vsFile);
				}

				InputStream targetStream = new FileInputStream(profileFile);
				String content = IOUtils.toString(targetStream);
				String cleanedContent = packagingHandler.removeUnusedAndDuplicateMessages(content, wrapper.getTestcases());
				FileUtils.writeStringToFile(profileFile, cleanedContent);
				packagingHandler.changeProfileId(profileFile);
				files.add(profileFile);

				FileUtils.writeStringToFile(jsonFile, testCaseJson.toString());
				files.add(jsonFile);
				zip = packagingHandler.zip(files,
				request.getServletContext().getRealPath("tmp/" + wrapper.getToken() + "/" + userId + "/testcase.zip"));
//			}

			if (zip != null) {
				//fix this step no need to zip to unzip just after...
				String directory2 = bundleHandler.unzip(Files.toByteArray(zip));
				GVTSaveInstance si = bundleHandler.unbundle(directory2);
				ipRepository.save(si.ip);
				csRepository.save(si.ct);
				vsRepository.save(si.vs);
				si.tcg.setUserId(userId);
				testCaseGroupRepository.saveAndFlush(si.tcg);
 				FileUtils.deleteDirectory(new File(request.getServletContext().getRealPath("tmp/" + wrapper.getToken() + "/"  + userId)));
				return new UploadStatus(ResourceUploadResult.SUCCESS, "Test Cases Group has been added");
			} else {
				return new UploadStatus(ResourceUploadResult.FAILURE, "Submitted bundle is empty");
			}
		} catch (IOException e) {
			return new UploadStatus(ResourceUploadResult.FAILURE, "IO Error could not read bundle");
		} catch (NoUserFoundException e) {
			return new UploadStatus(ResourceUploadResult.FAILURE, "No User Found");
		} catch (Exception e) {
			e.printStackTrace();
			return new UploadStatus(ResourceUploadResult.FAILURE,
					"Could not read bundle, " + "please check that format is correct");
		}

	}
	
	@PreAuthorize("hasRole('tester')")
	@RequestMapping(value = "/deletetestcase", method = RequestMethod.POST)
	@ResponseBody
	@Transactional(value = "transactionManager")
	public boolean deletetestcase(ServletRequest request, @RequestBody LongResult lr, Principal p) throws NoUserFoundException{
		Long userId = userIdService.getCurrentUserId(p);
		
		if(userId == null){
			throw new NoUserFoundException();
		}
				
		List<UserTestCaseGroup> list = testCaseGroupRepository.userExclusive(userId);
		for (UserTestCaseGroup utg : list){
			
			for (Iterator<UserCFTestInstance> iterator =  utg.getTestCases().iterator(); iterator.hasNext();) {
				UserCFTestInstance ucf = iterator.next();
				if (ucf.getId().equals(lr.getId())){
					 iterator.remove();
				}
			}
							
			if (utg.getTestCases().size() == 0 ){
				testCaseGroupRepository.delete(utg);
			}else{
				testCaseGroupRepository.save(utg);
			}
		}
		
			return true;
	}
	
	
	

}
