package gov.nist.hit.gvt.service.impl;

import gov.nist.auth.hit.core.domain.Account;
import gov.nist.hit.core.service.AccountService;
import gov.nist.hit.core.service.UserService;
import gov.nist.hit.gvt.service.UserIdService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
public class UserIdServiceImpl implements UserIdService{

	@Autowired
	private UserService userService;
	
	@Autowired
	private AccountService accountService;
	
	@Override
	public Long getCurrentUserId() {
		User u = userService.getCurrentUser();
		if (u != null && u.isEnabled()) {
		      Account a = accountService.findByTheAccountsUsername(u.getUsername());
		      if(a != null && !a.isPending() && !a.isGuestAccount()){
		    	  return a.getId();
		      }
		}
		return null;
	}
	
}
