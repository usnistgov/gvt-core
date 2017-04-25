package gov.nist.hit.gvt.service.impl;

import java.io.File;
import java.io.InputStream;
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
import gov.nist.healthcare.resources.xds.XMLResourcesValidator;
import gov.nist.hit.core.service.ResourceLoader;
import gov.nist.hit.gvt.service.FileValidationHandler;

@Service
public class FileValidationHandlerImpl implements FileValidationHandler{

	@Autowired
	private ResourceLoader resourceLoader;
	
	
	public List<XMLError> validateProfile(InputStream contentIS) throws Exception{
		XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
		List<XMLError> errors = v.validateProfile(contentIS);
		return errors;
		
	}
	public List<XMLError> validateConstraints(InputStream contentIS) throws Exception{
		XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
		List<XMLError> errors = v.validateConstraints(contentIS);
		return errors;
	}
	
	public List<XMLError> validateVocabulary(InputStream contentIS) throws Exception{
		XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
		List<XMLError> errors = v.validateVocabulary(contentIS);
		return errors;
	}
	
	

	public Map<String, List<XMLError>> unbundleAndValidate(String dir) throws Exception{
		XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
		Map<String, List<XMLError>> errorsMap = new HashMap<String, List<XMLError>>();
		resourceLoader.setDirectory(findFileDirectory(dir,"Profile.xml")+"/");
		
		// Profile
		Resource profile = resourceLoader.getResource("Profile.xml");
		errorsMap.put("profileErrors",v.validateProfile(profile.getInputStream()));
		
		// Constraints
		Resource constraints = resourceLoader.getResource("Constraints.xml");
		errorsMap.put("constraintsErrors",v.validateConstraints(constraints.getInputStream()));
		
		// VS
		Resource vs = resourceLoader.getResource("ValueSets.xml");
		errorsMap.put("vsErrors",v.validateVocabulary(vs.getInputStream()));
		
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
