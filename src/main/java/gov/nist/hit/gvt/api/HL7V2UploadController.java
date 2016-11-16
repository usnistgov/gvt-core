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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
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
import gov.nist.hit.gvt.domain.UploadedProfileModel;
import hl7.v2.profile.Profile;
import hl7.v2.profile.XMLDeserializer;
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

  private final Map<String, ProfileModel> profileModelsMap = new HashMap<String, ProfileModel>();

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
  public boolean addtestcases(@RequestBody List<UploadedProfileModel> list) {
	  for (UploadedProfileModel upm : list){
		  
//		  TODO add selected testcase
//		  profileModelsMap.get(upm.getId());
	  }
    return true;
  }
 
  @RequestMapping(value = "/cleartestcases", method = RequestMethod.POST)
  @ResponseBody
  public boolean cleartestcases() {
	  profileModelsMap.clear();
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
  

}
