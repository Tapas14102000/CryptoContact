package com.smart.controller;

import javax.servlet.http.HttpSession;

import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.EmailService;
import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
public class ForgotController {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private EmailService emailService;
	@Autowired
	private BCryptPasswordEncoder bcrypt;
	
	
	@RequestMapping("/verify")
	public String openEmailForm(HttpSession session) {
		session.removeAttribute("message");
		return "forgot_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email,HttpSession s) {
		if(this.userRepository.getUserByUserName(email)==null) {
			s.setAttribute("message", "Email ID not Registered");
			return "forgot_email_form";}
		System.out.println("EMAIL : "+email);
		int otp=(int) (Math.random()*9000)+1000;
		System.out.println("Otp = "+otp);
		
		String subject="Smart Contact Manager - OTP";
		String message=""
		+"<div style='border:1px solid #e2e2e2; padding:20px'>"
		+"<h1>"
		+"OTP is"
		+"<b> "+otp
		+"</b>"
		+"</h1>"
		+"</div>";
		String to=email;
		boolean flag=this.emailService.sendEmail(subject, message, to);
		if(flag) {
			s.setAttribute("myotp", otp);
			s.setAttribute("email", email);
			s.removeAttribute("message");
			return "verify_otp";
		}
		else {
			s.setAttribute("message", "Internet Not Connected");
			return "forgot_email_form";
		}
	}
	
	@PostMapping("/verify-otp")
	public String verifyOTP(@RequestParam("otp")int otp,HttpSession session) {
		int myotp=(int)session.getAttribute("myotp");
		if(myotp==otp) {
			session.removeAttribute("message");
			return "password_change_form";
		
		}else {
			session.setAttribute("message", "You Have entered Wrong OTP !");
			return "verify_otp";
		}
	}
	@PostMapping("/verify-email")
	public String verifyEMAIL(@RequestParam("otp")int otp,HttpSession session,Model m) {
		int myotp=(int)session.getAttribute("myotp");
		if(myotp==otp) {
			System.out.println("hello\n\n\n\n");
			User user=(User)session.getAttribute("user");
			try {
			this.userRepository.save(user);
			m.addAttribute("user",new User());
			session.setAttribute("message", new Message("Successfully Registered !!","alert-success"));
			return "signup";}
			catch (Exception e) {
				//e.printStackTrace();
				m.addAttribute("user",(User)session.getAttribute("user"));
				if(e.getMessage().equalsIgnoreCase("could not execute statement; SQL [n/a]; constraint [user.UK_ob8kqyqqgmefl0aco34akdtpe]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement")) {
					m.addAttribute("user", user);
					session.setAttribute("message", new Message("Mail Already Registered !!","alert-danger"));
				}else {
					session.setAttribute("message", new Message(e.getMessage(),"alert-danger"));	
				}
				return "signup";
			}
		
		}else {
			session.setAttribute("message", "You Have entered Wrong OTP !");
			return "verify_email";
		}
	}
	
	@PostMapping("/change-password")
	public String changepassword(@RequestParam("newpassword")String newpassword,HttpSession session) {
		String email=(String)session.getAttribute("email");
		User user=this.userRepository.getUserByUserName(email);
		user.setPassword(this.bcrypt.encode(newpassword));
		this.userRepository.save(user);
		return "redirect:/signin?change=password change successfully..";
	}
}
