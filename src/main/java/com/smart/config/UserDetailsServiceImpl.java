package com.smart.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.smart.dao.UserRepository;
import com.smart.entities.User;

public class UserDetailsServiceImpl implements UserDetailsService{
	@Autowired
	private UserRepository us;
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user=us.getUserByUserName(username);
		if(user==null)
			throw new UsernameNotFoundException("Couldn't found user!");
		CustomUserDetails cust=new CustomUserDetails(user);
		return cust;
	}

}