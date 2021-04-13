package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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
import org.springframework.web.multipart.MultipartFile;

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
	public String home(Model m,Principal p,HttpSession s) {
		m.addAttribute("title", "Home-CryptoContact");
		if(p!=null)
		System.out.println("homep : "+p.getName());
		if(s.getAttribute("key")!=null)
			System.out.println("homes : "+s.getAttribute("key"));
		return "home";
	}

	@RequestMapping("/{name}/sendrequest")
	public String sendRequest(@PathVariable("name") String name, Model m,HttpSession session) {
		session.removeAttribute("name");
		System.out.println("name = " + name);
		User user = this.userRepository.getUserByUserName(name);
		System.out.println(user);
		if (user != null) {
			m.addAttribute("name", name);
			session.setAttribute("name", name);}
		return "sendRequest";
	}

	// request handle
	@PostMapping("/process-contact")
	public String processContact(@Valid @ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file, Model m, HttpSession session) {
		session.removeAttribute("message");
		String name=null;
		if(session.getAttribute("name")==null)
			return "ErrorSendRequest";
		name = session.getAttribute("name").toString();
		User user = this.userRepository.getUserByUserName(name);
		contact.setUser(user);
		contact.setImage("contact.png");
		contact.setAdded(false);
		user.getContacts().add(contact);
		this.userRepository.save(user);
		try {
			Contact contact1=this.contactRepository.findByPhoneContainingAndUser(contact.getPhone(), user);
		if (!file.isEmpty()) {
			System.out.println("file changed");
			contact1.setImage(file.getOriginalFilename());
			File savefFile = new ClassPathResource("static/img").getFile();
			System.out.println("\n\n" + contact1.getPhone() + file.getOriginalFilename() + "\n\n");
			Path path1 = Paths.get(
					savefFile.getAbsolutePath() + File.separator + contact1.getcId() + file.getOriginalFilename());
			Files.copy(file.getInputStream(), path1, StandardCopyOption.REPLACE_EXISTING);
		this.contactRepository.save(contact1);
		}}
		catch (Exception e) {
			System.out.println("while setting photo");
		}
		String subject = "CryptoContact - Contact Request";
		String message=com.smart.message.before+"Contact Request"+com.smart.message.middle+user.getName()+com.smart.message.mid2+contact.getName()+" "+contact.getSecondName()+"("+contact.getEmail()+") has sent you a friend Request !"+com.smart.message.end;
		String to = name;
		boolean flag = this.emailService.sendEmail(subject, message, to);
		session.setAttribute("message", new Message("Request Sent Successfully", "success"));
		System.out.println("redirect:/" + name + "/sendRequest");
		return "redirect:/" + name + "/sendrequest";
	}

	@RequestMapping("/about")
	public String about(Model m) {
		m.addAttribute("title", "About-CryptoContact");
		return "about";
	}

	@RequestMapping("/signup")
	public String signup(Model m, HttpSession s) {
		if (s.getAttribute("message") != null)
			s.removeAttribute("message");
		m.addAttribute("title", "Register-CryptoContact");
		m.addAttribute("user", new User());
		return "signup";
	}

	// handler of register user
	@PostMapping("/do_register")
	public String registerUser(@Valid @ModelAttribute("user") User user,BindingResult result1, @RequestParam("key") String key, @RequestParam(value = "agreement", defaultValue = "false") Boolean agreement,
			Model m, HttpSession s) {
		if (!agreement) {
			System.out.println("You haven't agreed the terms and conditions..");
		}
		if (result1.hasErrors()) {
			s.removeAttribute("message");
			System.out.println("Error : " + result1.toString());
			m.addAttribute("user", user);
			return "signup";
		}
		StrongTextEncryptor ste = new StrongTextEncryptor();
		ste.setPassword(key);
		user.setRole("ROLE_USER");
		user.setEnabled(true);
		user.setImageUrl("contact.png");
		user.setAbout(ste.encrypt(user.getAbout()));
		user.setPassword(bcrypt.encode(user.getPassword()));
		System.out.println("Agreement=" + agreement + "\nUser=" + user);
		s.setAttribute("user", user);
		int otp = (int) (Math.random() * 9000) + 1000;
		System.out.println("Otp = " + otp);

		String subject = "CryptoContact - OTP";
		String message=com.smart.message.before+"OTP"+com.smart.message.middle+user.getName()+com.smart.message.mid2+"YOUR OTP IS <h3 style=\"color: rgb(45, 154, 187);\">"+otp+"</h3>"+com.smart.message.end;
		String to = user.getEmail();
		boolean flag = this.emailService.sendEmail(subject, message, to);
		System.out.println("flag = "+flag);
		if (flag) {
			s.setAttribute("myotp", otp);
			s.setAttribute("email", user.getEmail());
			s.removeAttribute("message");
			return "verify_email";
		} else {
			s.setAttribute("message", "Internet not Connected!");
			return "signup";
		}
	}

	// handler for custom login
	@GetMapping("/signin")
	public String customLogin(Model m,HttpSession session,Principal p) {
		m.addAttribute("title", "Login Page");
		System.out.println("Sign in Page Email : "+session.getAttribute("email"));
		if(p!=null)
		System.out.println("Sign in Page Principal : "+p.getName());
		else
			System.out.println("Sign in Page Principal : NULL");
		if(session.getAttribute("email")==null||p==null)
		return "login";
		//return "redirect:/user/profile";
		return "errorlogin";
	}
	@GetMapping("/logout")
	public String logout() {
		System.out.println("logout");
		return "redirect:/signin?logout";
	}
	

	@GetMapping("/check_key")
	public String checkkey(Model m,HttpSession session,Principal principal) {
		session.setAttribute("email", principal.getName());
		m.addAttribute("title", "Secret Key");
		if(session.getAttribute("key")!=null)
			return "errorlogin";
		return "check_key";
	}

	@PostMapping("/send-email")
	public String sendmail(@RequestParam("name") String name, @RequestParam("email") String email,
			@RequestParam("comments") String comment, HttpSession session) {
		String message = "" + "<div style='border:1px solid #e2e2e2; padding:20px' class=\"bg-dark text-white\">"
				+ "<h3>" + "From : " + "<b>" + name + "</b>" + "</h3>" + "<h3>" + "Mail : " + "<b>" + email + "</b>"
				+ "</h3>" + "<h3>" + "Message : " + "<b> " + comment + "</b>" + "</h3>" + "</div>";
		boolean flag = this.emailService.sendEmail(name, message, "tdkdevelopment14@gmail.com");
		if (flag)
			session.setAttribute("message",
					new Message("We got the message , We will get in touch with you soon !", "alert-success"));
		else
			session.setAttribute("message", new Message("Internet not connected !", "alert-danger"));
		System.out.println("hello");
		return "redirect:/";
	}

}
