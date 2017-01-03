package gov.nist.hit.gvt.service;

import java.io.File;
import java.io.IOException;

import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.gvt.domain.GVTSaveInstance;

public interface BundleHandler {

	public String unzip(byte[] bytes) throws Exception;
	public GVTSaveInstance unbundle(String dir) throws IOException, ProfileParserException;
	
	public String getProfileContentFromZipDirectory(String dir) throws IOException;
	public String getValueSetContentFromZipDirectory(String dir) throws IOException;
	public String getConstraintContentFromZipDirectory(String dir) throws IOException;	
	
}
