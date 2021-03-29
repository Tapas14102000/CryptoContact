package com.smart.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.EmailService;
import com.smart.dao.UserRepository;
import com.smart.dao.contactRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
public class HomeController {
	@Autowired
	private BCryptPasswordEncoder bcrypt;
	@Autowired
	private EmailService emailService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private contactRepository contactRepository;
	@RequestMapping("/")
	public String home(Model m) {
		m.addAttribute("title","Home-Smart Contact Manager");
		return "home";
	}
	@RequestMapping("/{name}/sendrequest")
	public String sendRequest(@PathVariable("name")String name,HttpSession session) {
		session.removeAttribute("name");
		System.out.println("name = "+name);
		User user=this.userRepository.getUserByUserName(name);
		System.out.println(user);
		if(user!=null)
		session.setAttribute("name", name);
		return "sendRequest";
	}
	
	//request handle
	@PostMapping("/process-contact")
	public String processContact(@Valid @ModelAttribute Contact contact,Model m,HttpSession session) {
		session.removeAttribute("message");
		String name = null;
		try {
			StrongTextEncryptor ste=new StrongTextEncryptor();
			ste.setPassword((String)session.getAttribute("key"));
			name=session.getAttribute("name").toString();
		User user=this.userRepository.getUserByUserName(name);
		contact.setUser(user);
		System.out.println("contact.getPhone() : "+contact.getPhone());
		System.out.println("key = "+(String)session.getAttribute("key"));
		Contact con=this.contactRepository.findByPhoneContainingAndUser(ste.encrypt(contact.getPhone()), user);
		if(con!=null)
			throw new Exception("Same Contact Found");
		//processing and uploading file
			contact.setImage("contact.png");
			contact.setAdded(false);
		user.getContacts().add(contact);
		this.userRepository.save(user);
		session.setAttribute("message", new Message("Request Sent Successfully", "success"));
		}
		catch(Exception e) {
			session.setAttribute("message", new Message("Same Contact Found", "danger"));	
		}
		System.out.println("redirect:/"+name+"/sendRequest");
		return "redirect:/"+name+"/sendrequest";
	}

	
	@RequestMapping("/about")
	public String about(Model m) {
		m.addAttribute("title","About-Smart Contact Manager");
		return "about";
	}
	@RequestMapping("/signup")
	public String signup(Model m,HttpSession s) {
		if(s.getAttribute("message")!=null)
		s.removeAttribute("message");
		m.addAttribute("title","Register-Smart Contact Manager");
		m.addAttribute("user",new User());
		return "signup";
	}
	
	//handler of register user
	@PostMapping("/do_register")
	public String registerUser(@Valid @ModelAttribute("user") User user,@RequestParam("key")String key,BindingResult result1,
			@RequestParam(value = "agreement",defaultValue = "false")Boolean agreement,Model m,
			HttpSession s) {
			if(!agreement) {
				System.out.println("You haven't agreed the terms and conditions..");
			}
			if(result1.hasErrors()) {
				System.out.println("Error : "+result1.toString());
				m.addAttribute("user",user);
				return "signup";
			}
			StrongTextEncryptor ste=new StrongTextEncryptor();
			ste.setPassword(key);
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("contact.png");
			user.setAbout(ste.encrypt(user.getAbout()));
			user.setPassword(bcrypt.encode(user.getPassword()));
			System.out.println("Agreement="+agreement+"\nUser="+user);
			s.setAttribute("user", user);
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
			String to=user.getEmail();
			boolean flag=this.emailService.sendEmail(subject, message, to);
			if(flag) {
				s.setAttribute("myotp", otp);
				s.setAttribute("email", user.getEmail());
				s.removeAttribute("message");
				return  "verify_email";
			}
			else {
				s.setAttribute("message", "Internet not Connected!");
				return "signup";
			}
	}
	
	//handler for custom login
	@GetMapping("/signin")
	public String customLogin(Model m) {
		m.addAttribute("title", "Login Page");
		return "login";
	}
	@GetMapping("/check_key")
	public String checkkey(Model m) {
		m.addAttribute("title", "Secret Key");
		return "check_key";
	}
	
	@PostMapping("/send-email")
	public String sendmail(@RequestParam("name")String name,@RequestParam("email")String email,@RequestParam("comments")String comment,HttpSession session) {
		String message=""
				+"<div style='border:1px solid #e2e2e2; padding:20px' class=\"bg-dark text-white\">"
				+"<h3>"
				+"From : "
				+"<b>"+name
				+"</b>"
				+"</h3>"
				+"<h3>"
				+"Mail : "
				+"<b>"+email
				+"</b>"
				+"</h3>"
				+"<h3>"
				+"Message : "
				+"<b> "+comment
				+"</b>"
				+"</h3>"
				+"</div>";
		boolean flag = this.emailService.sendEmail(name, message, "tdkdevelopment14@gmail.com");
		if(flag) 
			session.setAttribute("message", new Message("We got the message , We will get in touch with you soon !","alert-success"));
		else
			session.setAttribute("message", new Message("Internet not connected !","alert-danger"));
		System.out.println("hello");
		return "redirect:/";
	}
	
	
}
