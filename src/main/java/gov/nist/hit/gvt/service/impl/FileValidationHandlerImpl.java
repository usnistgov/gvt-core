package gov.nist.hit.gvt.service.impl;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import gov.nist.healthcare.resources.domain.XMLError;
import gov.nist.healthcare.resources.xds.ValidateXSD;
import gov.nist.hit.core.service.ResourceLoader;
import gov.nist.hit.core.service.util.FileUtil;
import gov.nist.hit.gvt.service.FileValidationHandler;

@Service
public class FileValidationHandlerImpl implements FileValidationHandler{

	@Autowired
	private ResourceLoader resourceLoader;
	
	
	public List<XMLError> validateProfile(String content) throws Exception{
		ValidateXSD v = new ValidateXSD();
		List<XMLError> errors = v.validateProfile(content);
		return errors;
		
	}
	public List<XMLError> validateConstraints(String content) throws Exception{
		ValidateXSD v = new ValidateXSD();
		List<XMLError> errors = v.validateConstraints(content);
		return errors;
	}
	
	public List<XMLError> validateVocabulary(String content) throws Exception{
		ValidateXSD v = new ValidateXSD();
		List<XMLError> errors = v.validateVocabulary(content);
		return errors;
	}
	
	

	public Map<String, List<XMLError>> unbundleAndValidate(String dir) throws Exception{
		ValidateXSD v = new ValidateXSD();
		Map<String, List<XMLError>> errorsMap = new HashMap<String, List<XMLError>>();
		resourceLoader.setDirectory(findFileDirectory(dir,"Profile.xml")+"/");
		
		// Profile
		Resource profile = resourceLoader.getResource("Profile.xml");
		String profileContent = FileUtil.getContent(profile);
		errorsMap.put("profileErrors",v.validateProfile(profileContent));
		
		// Constraints
		Resource constraints = resourceLoader.getResource("Constraints.xml");
		String constraintsContent = FileUtil.getContent(constraints);
		errorsMap.put("constraintsErrors",v.validateConstraints(constraintsContent));
		
		// VS
		Resource vs = resourceLoader.getResource("ValueSets.xml");
		String vsContent = FileUtil.getContent(vs);
		errorsMap.put("vsErrors",v.validateVocabulary(vsContent));
		
		return errorsMap;
	}
	
	// finds folder where file is found (first occurence)
	private String findFileDirectory(String dir, String fileName) {
		Collection files = FileUtils.listFiles(new File(dir), null, true);
		for (Iterator iterator = files.iterator(); iterator.hasNext();) {
			File file = (File) iterator.next();
			if (file.getName().equals(fileName)) {
				return file.getParentFile().getAbsolutePath();
			}
		}
		return null;
	}
	
}
