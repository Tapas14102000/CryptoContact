package com.smart.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.smart.dao.UserRepository;
import com.smart.dao.contactRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;

@RestController
public class SearchController {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private contactRepository contactRepository;
	
	@ModelAttribute // this will work for every handler
	public void addCommomData(Model m, Principal principal) {
		String Username = principal.getName();
		User user = userRepository.getUserByUserName(Username);
		m.addAttribute("user", user);
		m.addAttribute("email", principal.getName());
	}
	
	@GetMapping("/search/{query}")
	public ResponseEntity<?> search(@PathVariable("query")String query,Model m){
		String email=m.getAttribute("email").toString();
		System.out.println("search query : "+email);
		User user=this.userRepository.getUserByUserName(email);
		List<Contact> contacts=this.contactRepository.findByNameContainingAndUser(query, user);
		
		return ResponseEntity.ok(contacts);
	}
}
