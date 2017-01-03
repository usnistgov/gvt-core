package gov.nist.hit.gvt.service;

import java.io.File;
import java.util.List;

import gov.nist.hit.gvt.domain.UploadedProfileModel;

public interface PackagingHandler {

	public List<UploadedProfileModel> getUploadedProfiles(String xml);
	public String removeUnusedAndDuplicateMessages(String content,List<UploadedProfileModel> presentMessages);
	public File changeProfileId(File file)  throws Exception;
	public File changeConstraintId(File file)  throws Exception;
	public File changeVsId(File file)  throws Exception;
	public  File zip(List<File> files, String filename) throws Exception;
	
	
}
