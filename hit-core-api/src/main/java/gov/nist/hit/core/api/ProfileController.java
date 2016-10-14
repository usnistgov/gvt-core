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

import gov.nist.hit.core.domain.Json;
import gov.nist.hit.core.repo.ConformanceProfileRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Harold Affo (NIST)
 * 
 */

@RequestMapping("/profile")
@RestController
@Api(value = "Conformance profiles api", tags = "Conformance Profiles")
public class ProfileController {

  Logger logger = LoggerFactory.getLogger(ProfileController.class);

  @Autowired
  private ConformanceProfileRepository conformanceProfileRepository;

  @ApiOperation(value = "Get the json representation of a conformance profile by its id",
      nickname = "getProfileJsonById")
  @RequestMapping(value = "/{profileId}", method = RequestMethod.GET, produces = "application/json")
  public Json getProfileJsonById(@ApiParam(value = "the id of the conformance profile",
      required = true) @PathVariable final long profileId) {
    logger.info("Fetching conformance profile (json) with id=" + profileId);
    String value = conformanceProfileRepository.getJson(profileId);
    return new Json(value);
  }



}
