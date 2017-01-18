package gov.nist.hit.gvt.service;

import java.security.Principal;

public interface UserIdService {
	public Long getCurrentUserId(Principal p);
}
