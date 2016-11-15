package gov.nist.hit.gvt.service;

import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.gvt.domain.GVTTestCaseGroup;

import java.io.IOException;

public interface BundleHandler {

	public String unzip(byte[] bytes) throws Exception;
	public GVTTestCaseGroup unbundle(String dir) throws IOException, ProfileParserException;
}
