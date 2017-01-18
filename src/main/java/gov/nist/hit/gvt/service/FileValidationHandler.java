package gov.nist.hit.gvt.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import gov.nist.healthcare.resources.domain.XMLError;

public interface FileValidationHandler {

	public List<XMLError> validateProfile(String content) throws Exception;
	public List<XMLError> validateConstraints(String content) throws Exception;
	public List<XMLError> validateVocabulary(String content) throws Exception;
	
	public Map<String, List<XMLError>> unbundleAndValidate(String dir) throws Exception;
}
