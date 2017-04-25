package gov.nist.hit.gvt.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import gov.nist.healthcare.resources.domain.XMLError;

public interface FileValidationHandler {

	public List<XMLError> validateProfile(InputStream contentIS) throws Exception;
	public List<XMLError> validateConstraints(InputStream contentIS) throws Exception;
	public List<XMLError> validateVocabulary(InputStream contentIS) throws Exception;
	
	public Map<String, List<XMLError>> unbundleAndValidate(String dir) throws Exception;
}
