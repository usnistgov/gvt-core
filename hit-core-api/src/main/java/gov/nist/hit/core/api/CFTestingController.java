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

package gov.nist.hit.core.api;

import gov.nist.hit.core.domain.TestObject;
import gov.nist.hit.core.repo.TestObjectRepository;
import gov.nist.hit.core.service.TestObjectService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Harold Affo (NIST)
 * 
 */
@RequestMapping("/cf")
@RestController
public class CFTestingController {

  static final Logger logger = LoggerFactory.getLogger(CFTestingController.class);

  @Autowired
  private TestObjectService testObjectService;

  @Cacheable(value = "testCaseCache", key = "'cf-testcases'")
  @RequestMapping(value = "/testcases")
  public List<TestObject> testCases() {
    logger.info("Fetching all testCases...");
    return testObjectService.findAllAsRoot();
  }

  public TestObjectService getTestObjectService() {
    return testObjectService;
  }

  public void setTestObjectService(TestObjectService testObjectService) {
    this.testObjectService = testObjectService;
  }



}
