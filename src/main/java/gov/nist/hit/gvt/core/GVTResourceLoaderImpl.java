package gov.nist.hit.gvt.core;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.domain.ResourceUploadStatus;
import gov.nist.hit.core.domain.TestCaseDocument;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.domain.TestScope;
import gov.nist.hit.core.domain.TestingStage;
import gov.nist.hit.core.domain.VocabularyLibrary;
import gov.nist.hit.core.hl7v2.service.HL7V2ResourceLoader;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.xml.XMLResourceLoader;

public class GVTResourceLoaderImpl extends GVTResourceLoader {

	@Autowired
	@Qualifier("hl7v2ResourceLoader")
	HL7V2ResourceLoader hl7v2rb;

	@Autowired
	@Qualifier("xmlResourceLoader")
	XMLResourceLoader xmlrb;

	@Override
	public List<ResourceUploadStatus> addOrReplaceValueSet(String rootPath, String domain, TestScope scope,
			String username, boolean preloaded) throws IOException {
		return hl7v2rb.addOrReplaceValueSet(rootPath, domain, scope, username, preloaded);
	}

	@Override
	public List<ResourceUploadStatus> addOrReplaceConstraints(String rootPath, String domain, TestScope scope,
			String username, boolean preloaded) throws IOException {
		return hl7v2rb.addOrReplaceConstraints(rootPath, domain, scope, username, preloaded);
	}

	@Override
	public List<ResourceUploadStatus> addOrReplaceIntegrationProfile(String rootPath, String domain, TestScope scope,
			String username, boolean preloaded) throws IOException {
		return hl7v2rb.addOrReplaceIntegrationProfile(rootPath, domain, scope, username, preloaded);
	}

	@Override
	public TestContext testContext(String location, JsonNode parentOb, TestingStage stage, String rootPath,
			String domain, TestScope scope, String authorUsername, boolean preloaded) throws Exception {
		if (!parentOb.findValues("hl7v2").isEmpty()) {
			return hl7v2rb.testContext(location, parentOb, stage, rootPath, domain, scope, authorUsername, preloaded);
		}
		if (!parentOb.findValues("xml").isEmpty()) {
			return xmlrb.testContext(location, parentOb, stage, rootPath, domain, scope, authorUsername, preloaded);
		}

		return null;
	}

	@Override
	public TestCaseDocument generateTestCaseDocument(TestContext c) throws IOException {
		if ("hl7v2".equals(c.getFormat())) {
			return hl7v2rb.generateTestCaseDocument(c);
		}
		if ("xml".equals(c.getFormat())) {
			return xmlrb.generateTestCaseDocument(c);
		}
		return new TestCaseDocument();
	}

	@Override
	public ProfileModel parseProfile(String integrationProfileXml, String conformanceProfileId, String constraintsXml,
			String additionalConstraintsXml) throws ProfileParserException, UnsupportedOperationException {
		return hl7v2rb.parseProfile(integrationProfileXml, conformanceProfileId, constraintsXml,
				additionalConstraintsXml);
	}

	@Override
	public VocabularyLibrary vocabLibrary(String content, String domain, TestScope scope, String authorUsername,
			boolean preloaded)
			throws JsonGenerationException, JsonMappingException, IOException, UnsupportedOperationException {		
		return hl7v2rb.vocabLibrary(content, domain, scope, authorUsername, preloaded);
	}

}
