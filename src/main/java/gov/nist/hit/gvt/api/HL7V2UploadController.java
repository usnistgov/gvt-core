/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 */

package gov.nist.hit.gvt.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import gov.nist.healthcare.resources.domain.XMLError;
import gov.nist.healthcare.resources.xds.ValidateXSD;
import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.hl7v2.service.HL7V2ProfileParserImpl;
import gov.nist.hit.core.service.ProfileParser;
import gov.nist.hit.core.service.exception.MessageUploadException;
import gov.nist.hit.gvt.domain.TestCaseWrapper;
import gov.nist.hit.gvt.domain.UploadedProfileModel;
import io.swagger.annotations.Api;

/**
 * @author Nicolas Crouzier (NIST)
 * 
 */
@RequestMapping("/gvtupload")
@Api(hidden = true)
@Controller
public class HL7V2UploadController {

  static final Logger logger = LoggerFactory.getLogger(HL7V2UploadController.class);
  
  ProfileParser parser = new HL7V2ProfileParserImpl();

  private final TreeMap<String, File> profileFileMap = new TreeMap<String, File>();
  private final List<File> valueSetFileList = new ArrayList<File>();
  private final List<File> constraintFileList = new ArrayList<File>();

  @RequestMapping(value = "/uploadprofile", method = RequestMethod.POST,  consumes = {"multipart/form-data"})
  @ResponseBody
  public Map<String, Object> uploadprofile(@RequestPart("file") MultipartFile part)
      throws MessageUploadException {
    try {
      ValidateXSD v = new ValidateXSD();
      Map<String, Object> map = new HashMap<String, Object>();
      InputStream in = part.getInputStream();
      String content = IOUtils.toString(in);
      List<XMLError> errors = v.validateProfile(content); 
      
      if (errors.size() >0){
    	  map.put("success", false); 
    	  map.put("errors", errors);    	  
    	  logger.info("Uploaded profile file with errors "+part.getName());
      }else{
    	  List<UploadedProfileModel>  list = getUploadedProfiles(content);
    	  map.put("success", true); 
          map.put("profiles", list); 
          for (UploadedProfileModel upm : list){
        	  File profileFile = new File(part.getName());
        	  FileUtils.writeStringToFile(profileFile,content);
              profileFileMap.put(upm.getId(), profileFile);
          }
          logger.info("Uploaded valid profile file "+part.getName());
      }
      
      return map;   
    } catch (RuntimeException e) {
    	e.printStackTrace();
      throw new MessageUploadException(e);
    } catch (URISyntaxException e) {
		e.printStackTrace();
		throw new MessageUploadException(e);
	} catch (IOException e) {
		e.printStackTrace();
		throw new MessageUploadException(e);
	} 
  }
  
  @RequestMapping(value = "/addtestcases", method = RequestMethod.POST)
  @ResponseBody
  public boolean addtestcases(@RequestBody TestCaseWrapper wrapper) {
	  JSONObject testCaseJson = new JSONObject();
	  testCaseJson.put("name", wrapper.getTestcasename());
	  testCaseJson.put("description", wrapper.getTestcasedescription());
	  testCaseJson.put("profile",profileFileMap.firstEntry().getValue().getName());
	  if (!constraintFileList.isEmpty()){
		  testCaseJson.put("constraints",constraintFileList.get(0).getName());  
	  }
	  if (!valueSetFileList.isEmpty()){
		  testCaseJson.put("vs",valueSetFileList.get(0).getName());
	  }
	  JSONArray testSteps = new JSONArray();
	  for (UploadedProfileModel upm : wrapper.getTestcases()){
		  profileFileMap.get(upm.getId());
		  JSONObject ts = new JSONObject();
		  ts.put("name",upm.getName());
		  ts.put("messageId",upm.getId());
		  testSteps.put(ts);
	  }
	  testCaseJson.put("testCases", testSteps);
	  File jsonFile = new File("TestCases.json");
	  
	  
	  List<File> files = new ArrayList<File>();
	  try {
		  
		  if (!constraintFileList.isEmpty()){

			  files.add(valueSetFileList.get(0));
		  }
		  if (!valueSetFileList.isEmpty()){
			  files.add(valueSetFileList.get(0));
		  }
		  files.add(profileFileMap.firstEntry().getValue());
		    
		  FileUtils.writeStringToFile(jsonFile, testCaseJson.toString());
		  files.add(jsonFile);
	  
	  
	  } catch (IOException e) {
			e.printStackTrace();
		}
	  File zip = zip(files ,"testcase.xml");

	  //TODO send to api to add
	  return true;
  }
 
  

  
  
  
  @RequestMapping(value = "/cleartestcases", method = RequestMethod.POST)
  @ResponseBody
  public boolean cleartestcases() {
	  profileFileMap.clear();
	  valueSetFileList.clear();
	  constraintFileList.clear();
	  return true;
  }
  

  @RequestMapping(value = "/uploadvs", method = RequestMethod.POST,  consumes = {"multipart/form-data"})
  @ResponseBody
  public Map<String, Object> uploadvs(@RequestPart("file") MultipartFile part)
      throws MessageUploadException {
    try {	
    	ValidateXSD v = new ValidateXSD();
        Map<String, Object> map = new HashMap<String, Object>();
        InputStream in = part.getInputStream();
        String content = IOUtils.toString(in);
        List<XMLError> errors = v.validateVocabulary(content); 
        
        if (errors.size() >0){
      	  map.put("success", false); 
      	  map.put("errors", errors); 
      	  logger.info("Uploaded value set file with errors "+part.getName());
        }else{
      	  map.put("success", true);
      	  
      	  File vsFile = new File(part.getName());
      	  FileUtils.writeStringToFile(vsFile,content);
      	  valueSetFileList.add(vsFile);
          logger.info("Uploaded value set file "+part.getName());
        }     
        return map;   
    } catch (RuntimeException e) {
      throw new MessageUploadException(e);
    } catch (Exception e) {
      throw new MessageUploadException(e);
    }
  }

  @RequestMapping(value = "/uploadcontraints", method = RequestMethod.POST,  consumes = {"multipart/form-data"})
  @ResponseBody
  public Map<String, Object> uploadcontraints(@RequestPart("file") MultipartFile part)
      throws MessageUploadException {
    try {
    	ValidateXSD v = new ValidateXSD();
        Map<String, Object> map = new HashMap<String, Object>();
        InputStream in = part.getInputStream();
        String content = IOUtils.toString(in);
        List<XMLError> errors = v.validateConstraints(content); 
        
        if (errors.size() >0){
      	  map.put("success", false); 
      	  map.put("errors", errors); 
      	  logger.info("Uploaded constraints file with errors "+part.getName());
        }else{
      	  map.put("success", true);     
      	  
      	  File constraintFile = new File(part.getName());
    	  FileUtils.writeStringToFile(constraintFile,content);
      	  constraintFileList.add(constraintFile);
          logger.info("Uploaded constraints file "+part.getName());
        }     
        return map;   
    } catch (RuntimeException e) {
      throw new MessageUploadException(e);
    } catch (Exception e) {
      throw new MessageUploadException(e);
    }
  }

 
  
  public UploadedProfileModel initUploadedProfileModel(ProfileModel pm) {
	    UploadedProfileModel upm = new UploadedProfileModel();
	    upm.setActivated(false);
	    upm.setId(pm.getMessage().getId());
	    upm.setName(pm.getMessage().getName());
	    upm.setType(pm.getMessage().getType());
	    upm.setDescription(pm.getMessage().getDescription());
	    return upm;
	  }
  
  public List<UploadedProfileModel> getUploadedProfiles(String xml) {
	    Document doc = this.toDoc(xml);
	    NodeList nodes =  doc.getElementsByTagName("Message");
	    List<UploadedProfileModel> list = new ArrayList<UploadedProfileModel>();
	    for (int i = 0 ; i <nodes.getLength(); i++ ){
	    	Element elmIntegrationProfile =  (Element) nodes.item(i);	
	    	UploadedProfileModel upm = new UploadedProfileModel();
		    upm.setActivated(false);
		    upm.setId(elmIntegrationProfile.getAttribute("ID"));
		    upm.setName(elmIntegrationProfile.getAttribute("Name"));
		    upm.setType(elmIntegrationProfile.getAttribute("Type"));
		    upm.setEvent(elmIntegrationProfile.getAttribute("Event"));
		    upm.setStructID(elmIntegrationProfile.getAttribute("StructID"));
		    upm.setIdentifier(elmIntegrationProfile.getAttribute("Identifier"));
		    upm.setDescription(elmIntegrationProfile.getAttribute("Description"));
		    list.add(upm);
	    }
	    return list;
	  }
  
  private Document toDoc(String xmlSource) {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    factory.setIgnoringComments(false);
	    factory.setIgnoringElementContentWhitespace(true);
	    DocumentBuilder builder;
	    try {
	      builder = factory.newDocumentBuilder();
	      return builder.parse(new InputSource(new StringReader(xmlSource)));
	    } catch (ParserConfigurationException e) {
	      e.printStackTrace();
	    } catch (SAXException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return null;
	  }
  
  
  public  File zip(List<File> files, String filename) {
	    File zipfile = new File(filename);
	    // Create a buffer for reading the files
	    byte[] buf = new byte[1024];
	    try {
	        // create the ZIP file
	        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
	        // compress the files
	        for(int i=0; i<files.size(); i++) {
	            FileInputStream in = new FileInputStream(files.get(i).getName());
	            // add ZIP entry to output stream
	            out.putNextEntry(new ZipEntry(files.get(i).getName()));
	            // transfer bytes from the file to the ZIP file
	            int len;
	            while((len = in.read(buf)) > 0) {
	                out.write(buf, 0, len);
	            }
	            // complete the entry
	            out.closeEntry();
	            in.close();
	        }
	        // complete the ZIP file
	        out.close();
	        return zipfile;
	    } catch (IOException ex) {
	        System.err.println(ex.getMessage());
	    }
	    return null;
	}

}
