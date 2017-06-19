package gov.nist.hit.gvt.service;

import java.io.IOException;

import gov.nist.hit.core.domain.CFTestPlan;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.gvt.domain.GVTSaveInstance;

public interface BundleHandler {

	public String unzip(byte[] bytes, String path) throws Exception;
	public GVTSaveInstance createGVTSaveInstance(String dir) throws IOException, ProfileParserException;
	public GVTSaveInstance createGVTSaveInstance(String dir, CFTestPlan tp) throws IOException, ProfileParserException;
	
	public String getProfileContentFromZipDirectory(String dir) throws IOException;
	public String getValueSetContentFromZipDirectory(String dir) throws IOException;
	public String getConstraintContentFromZipDirectory(String dir) throws IOException;	
	
}
