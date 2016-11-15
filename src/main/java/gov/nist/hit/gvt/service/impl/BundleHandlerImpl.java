package gov.nist.hit.gvt.service.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import gov.nist.hit.core.domain.CFTestInstance;
import gov.nist.hit.core.domain.ConformanceProfile;
import gov.nist.hit.core.domain.Constraints;
import gov.nist.hit.core.domain.IntegrationProfile;
import gov.nist.hit.core.domain.VocabularyLibrary;
import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.service.ResourceLoader;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.util.FileUtil;
import gov.nist.hit.gvt.domain.GVTTestCaseGroup;
import gov.nist.hit.gvt.service.BundleHandler;

@Service
public class BundleHandlerImpl implements BundleHandler {
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@Override
	public String unzip(byte[] bytes) throws Exception {
		File tmpDir = Files.createTempDir();
		tmpDir.mkdir();
		if (tmpDir.isDirectory()) {
			// Extract ZIP
			ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));
			ZipEntry ze;
			while ((ze = zip.getNextEntry()) != null) {
				String filePath = tmpDir.getAbsolutePath() + File.separator
						+ ze.getName();
				if (!ze.isDirectory()) {
					BufferedOutputStream bos = new BufferedOutputStream(
							new FileOutputStream(filePath));
					byte[] bytesIn = new byte[1024];
					int read = 0;
					while ((read = zip.read(bytesIn)) != -1) {
						bos.write(bytesIn, 0, read);
					}
					bos.close();
				} else {
					File dir = new File(filePath);
					dir.mkdir();
				}
				zip.closeEntry();
			}
			zip.close();
			return tmpDir.getAbsolutePath();

		} else {
			throw new Exception("Could not create TMP directory");
		}
	}
	
	@Override
	public GVTTestCaseGroup unbundle(String dir) throws IOException, ProfileParserException{
		resourceLoader.setDirectory(dir+"/");
		Resource res = resourceLoader.getResource("TestCases.json");
		if(res == null){
			throw new IllegalArgumentException("No TestCases.json found");
		}
		GVTTestCaseGroup gtcg = new GVTTestCaseGroup();
		String descriptorContent = FileUtil.getContent(res);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode testCasesObj = mapper.readTree(descriptorContent);
		
		gtcg.setName(testCasesObj.findValue("name").asText());
		gtcg.setDescription(testCasesObj.findValue("description").asText());
		gtcg.setPreloaded(false);
		
		// Profile
		String profileName = testCasesObj.findValue("profile").asText();
		Resource profile = resourceLoader.getResource(profileName);
		if(profile == null){
			throw new IllegalArgumentException("Profile "+profileName+" not found");
		}
		IntegrationProfile p = resourceLoader.integrationProfile(FileUtil.getContent(profile));

		// Constraints
		String constraintName = testCasesObj.findValue("constraints").asText();
		Resource constraints = resourceLoader.getResource(constraintName);
		if(constraints == null){
			throw new IllegalArgumentException("Constraints "+constraintName+" not found");
		}
		Constraints c = resourceLoader.constraint(FileUtil.getContent(constraints));
		
		// VS
		String vocabName = testCasesObj.findValue("vs").asText();
		Resource vs = resourceLoader.getResource(vocabName);
		if(vs == null){
			throw new IllegalArgumentException("VocabularyLibrary "+vocabName+" not found");
		}
		VocabularyLibrary v = resourceLoader.vocabLibrary(FileUtil.getContent(vs));
		
		List<CFTestInstance> testCases = new ArrayList<>();
		Iterator<JsonNode> testCasesIter = testCasesObj.findValue("testCases").elements();
		while(testCasesIter.hasNext()){
			CFTestInstance cfti = new CFTestInstance();
			String messageId = testCasesObj.findValue("messageId").asText();
			String name = testCasesObj.findValue("name").asText();
			String description = testCasesObj.findValue("description").asText();
			
			//---
			ConformanceProfile conformanceProfile = new ConformanceProfile();
			conformanceProfile.setJson(
					resourceLoader.jsonConformanceProfile(p.getXml(), messageId, c.getXml(), null)
			);
			
			conformanceProfile.setIntegrationProfile(p);
			conformanceProfile.setSourceId(messageId);
			//---
			HL7V2TestContext testContext = new HL7V2TestContext();
			testContext.setVocabularyLibrary(v);
			testContext.setConstraints(c);
			testContext.setConformanceProfile(conformanceProfile);
			testContext.setDqa(false);
			//---
			cfti.setName(name);
			cfti.setDescription(description);
			cfti.setRoot(true);
			cfti.setTestContext(testContext);
			//---
			testCases.add(cfti);
		}
		
		gtcg.setTestCases(testCases);
		return gtcg;
	}
}
