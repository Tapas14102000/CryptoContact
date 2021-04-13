package com.smart.controller;

import java.io.File;
import com.razorpay.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.jasypt.util.text.StrongTextEncryptor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.dao.contactRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private contactRepository contactRepository;
	@Autowired
	private MyOrderRepository myOrderRepository;

	@ModelAttribute // this will work for every handler
	public void addCommomData(Model m, HttpSession s) {
		String Username = null;
		if (s.getAttribute("email") != null)
			Username = s.getAttribute("email").toString();
		User user = userRepository.getUserByUserName(Username);
		m.addAttribute("user", user);
		m.addAttribute("email", Username);
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model m, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		m.addAttribute("title", "Add Contact");
		m.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// open request form handler
	@GetMapping("/request-contact")
	public String openRequestContactForm(Model m) {
		m.addAttribute("title", "Request");
		return "normal/addrequest";
	}

	@PostMapping("/process-contact")
	public String processContact(@Valid @ModelAttribute Contact contact, Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		try {
			String email = session.getAttribute("email").toString();
			StrongTextEncryptor ste = new StrongTextEncryptor();
			ste.setPassword((String) session.getAttribute("key"));
			User user = this.userRepository.getUserByUserName(email);
			System.out.println();
			contact.setUser(user);
			System.out.println("contact.getPhone() : " + contact.getPhone());
			System.out.println("key = " + (String) session.getAttribute("key"));
			Contact con = this.contactRepository.findByPhoneContainingAndUser(ste.encrypt(contact.getPhone()), user);
			if (con != null)
				throw new Exception("Same Contact Found");
			// processing and uploading file
			contact.setImage("contact.png");
			contact.setEmail(ste.encrypt(contact.getEmail()));
			contact.setPhone(ste.encrypt(contact.getPhone()));
			contact.setWork(ste.encrypt(contact.getWork()));
			contact.setAdded(true);
			user.getContacts().add(contact);
			this.userRepository.save(user);
			session.setAttribute("message", new Message("Contact Added Successfully", "success"));
		} catch (Exception e) {
			session.setAttribute("message", new Message("Same Contact Found", "danger"));
		}
		return "normal/add_contact_form";
	}

	// show contact handler
	// per page=5 contacts
	// current page =0
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		String email = session.getAttribute("email").toString();
		m.addAttribute("title", "Contacts");
		Pageable pageable = PageRequest.of(page, 6);
		Page<Contact> contacts = this.contactRepository
				.findContactByUser(this.userRepository.getUserByUserName(email).getId(), pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		System.out.println("contacts.getTotalPages() = " + contacts.getTotalPages() + "\npage = " + page);
		return "normal/show_contacts";
	}

	@GetMapping("/request-contact/{page}")
	public String showrequest(@PathVariable("page") Integer page, Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		String email = session.getAttribute("email").toString();
		m.addAttribute("title", "Contacts");
		Pageable pageable = PageRequest.of(page, 6);
		Page<Contact> contacts = this.contactRepository
				.findContactByUserRequest(this.userRepository.getUserByUserName(email).getId(), pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		System.out.println("contacts.getTotalPages() = " + contacts.getTotalPages() + "\npage = " + page);
		return "normal/addrequest";
	}

	// show particular contact details
	@RequestMapping("/{name}/contact/{cId}")
	public String showcontact(@PathVariable("name") String name, @PathVariable("cId") Integer cId, Model m,
			HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		String email = session.getAttribute("email").toString();
		m.addAttribute("title", name + " Details");
		StrongTextEncryptor ste = new StrongTextEncryptor();
		try {
			ste.setPassword((String) session.getAttribute("key"));
			Optional<Contact> contactOptional = this.contactRepository.findById(cId);
			Contact contact = contactOptional.get();
			User user = this.userRepository.getUserByUserName(email);
			if (user.getId() == contact.getUser().getId()) {
				contact.setEmail(ste.decrypt(contact.getEmail()));
				contact.setWork(ste.decrypt(contact.getWork()));
				contact.setPhone(ste.decrypt(contact.getPhone()));
				m.addAttribute("contact", contact);
			}
		} catch (Exception e) {
		}
		return "normal/contact_detail";
	}

	@GetMapping("/delete/{cId}")
	public String DeleteContact(@PathVariable("cId") Integer cId, Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		try {
			String email = session.getAttribute("email").toString();
			System.out.println("\n\ncid=" + cId);
			Optional<Contact> contactOptional = this.contactRepository.findById(cId);
			Contact contact = contactOptional.get();
			User user = this.userRepository.getUserByUserName(email);
			if (user.getId() == contact.getUser().getId()) {
				contact.setUser(null);
				String x = contact.getcId() + contact.getImage();
				File savefFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(savefFile.getAbsolutePath() + File.separator + x);
				Files.deleteIfExists(path);
				System.out.println("contact deleted");
				this.contactRepository.delete(contact);
			}
		} catch (Exception e) {
			session.setAttribute("message", new Message("No such contacts Found", "danger"));
		}
		return "redirect:/user/show-contacts/0";
	}

	// delete requests
	@GetMapping("/deletes/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		try {
			String email = session.getAttribute("email").toString();
			System.out.println("\n\ncid=" + cId);
			Optional<Contact> contactOptional = this.contactRepository.findById(cId);
			Contact contact = contactOptional.get();
			User user = this.userRepository.getUserByUserName(email);
			if (user.getId() == contact.getUser().getId()) {
				contact.setUser(null);
				String x = contact.getcId() + contact.getImage();
				File savefFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(savefFile.getAbsolutePath() + File.separator + x);
				Files.deleteIfExists(path);
				System.out.println("contact deleted");
				this.contactRepository.delete(contact);
			}
		} catch (Exception e) {
			session.setAttribute("message", new Message("No such contacts Found", "danger"));
		}
		return "redirect:/user/request-contact/0";
	}

	// open update form handler for contacts
	@GetMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		try {
			String email = session.getAttribute("email").toString();
			StrongTextEncryptor ste = new StrongTextEncryptor();
			ste.setPassword((String) session.getAttribute("key"));
			m.addAttribute("title", "Update contact");
			Optional<Contact> contactOptional = this.contactRepository.findById(cId);
			Contact contact = contactOptional.get();
			User user = this.userRepository.getUserByUserName(email);
			if (user.getId() == contact.getUser().getId()) {
				contact.setEmail(ste.decrypt(contact.getEmail()));
				contact.setWork(ste.decrypt(contact.getWork()));
				contact.setPhone(ste.decrypt(contact.getPhone()));
				contact.setAdded(true);
				m.addAttribute("contact", contact);
			}
		} catch (Exception e) {
			m.addAttribute("contact", null);
		}
		return "normal/update_form";
	}

	// accept request
	@PostMapping("/add-contact/{cId}")
	public String addrequest(@PathVariable("cId") Integer cId, Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		StrongTextEncryptor ste = new StrongTextEncryptor();
		ste.setPassword((String) session.getAttribute("key"));
		Contact contact = this.contactRepository.findById(cId).get();
		contact.setEmail(ste.encrypt(contact.getEmail()));
		contact.setWork(ste.encrypt(contact.getWork()));
		contact.setPhone(ste.encrypt(contact.getPhone()));
		contact.setAdded(true);
		this.contactRepository.save(contact);
		return "redirect:/user/request-contact/0";
	}

	// update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, Model m,
			@RequestParam("profileImage") MultipartFile file, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		StrongTextEncryptor ste = new StrongTextEncryptor();
		String email = session.getAttribute("email").toString();
		try {
			Contact contact1 = this.contactRepository.findById(contact.getcId()).get();
			if (!file.isEmpty()) {
				System.out.println("file changed");
				// delete old photo and update new one
				String x = contact1.getcId() + contact1.getImage();
				File deleteFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(deleteFile.getAbsolutePath() + File.separator + x);
				Files.deleteIfExists(path);
				contact.setImage(file.getOriginalFilename());
				File savefFile = new ClassPathResource("static/img").getFile();
				System.out.println("\n\n" + contact.getPhone() + file.getOriginalFilename() + "\n\n");
				Path path1 = Paths.get(
						savefFile.getAbsolutePath() + File.separator + contact.getcId() + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path1, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("path=" + path);
			} else {
				System.out.println("file not changed");
				contact.setImage(contact1.getImage());
			}
			User user = this.userRepository.getUserByUserName(email);
			contact.setUser(user);
			ste.setPassword((String) session.getAttribute("key"));
			contact.setEmail(ste.encrypt(contact.getEmail()));
			contact.setWork(ste.encrypt(contact.getWork()));
			contact.setPhone(ste.encrypt(contact.getPhone()));
			contact.setAdded(true);
			this.contactRepository.save(contact);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "redirect:/user/" + contact.getName() + "/contact/" + contact.getcId();
	}

	@PostMapping("/check-key")
	public String secretkey(@RequestParam("key") String key, Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		StrongTextEncryptor ste = new StrongTextEncryptor();
		String email = session.getAttribute("email").toString();
		ste.setPassword(key);
		try {
			User user = this.userRepository.getUserByUserName(email);
			ste.decrypt(user.getAbout());
			System.out.println("valid key");
			session.setAttribute("key", key);

		} catch (Exception e) {
			System.out.println("invalid key");
		}
		return "redirect:/user/profile";
	}

	@GetMapping("/profile")
	public String yourProfile(Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		String email = session.getAttribute("email").toString();
		User user = this.userRepository.getUserByUserName(email);
		m.addAttribute("title", user.getName());
		if (session.getAttribute("key") != null) {
			StrongTextEncryptor ste = new StrongTextEncryptor();
			ste.setPassword((String) session.getAttribute("key"));
			user.setAbout(ste.decrypt(user.getAbout()));
		}
		m.addAttribute("user", user);
		return "normal/profile";
	}

	// open handler for user
	@GetMapping("/update-contact-user")
	public String updateFormUser(Model m, HttpSession session, Principal p) {
		if (p == null) {
			return "redirect:/signin?logout";
		}
		String email = session.getAttribute("email").toString();
		User user = this.userRepository.getUserByUserName(email);
		m.addAttribute("title", "Update " + user.getName());
		StrongTextEncryptor ste = new StrongTextEncryptor();
		ste.setPassword((String) session.getAttribute("key"));
		user.setAbout(ste.decrypt(user.getAbout()));
		m.addAttribute("user", user);
		return "normal/update_form_user";
	}

	// update contact handler
	@PostMapping("/process-update-user")
	public String updateHandlerUser(@ModelAttribute User user, Model m,
			@RequestParam("profileImages") MultipartFile file, HttpSession session, Principal p) {
		String email = session.getAttribute("email").toString();
		try {
			User user1 = this.userRepository.getUserByUserName(email);
			if (!file.isEmpty()) {
				System.out.println("file changed");
				// delete old photo and update new one
				String x = user1.getId() + user1.getImageUrl();
				File deleteFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(deleteFile.getAbsolutePath() + File.separator + x);
				Files.deleteIfExists(path);
				user.setImageUrl(file.getOriginalFilename());
				File savefFile = new ClassPathResource("static/img").getFile();
				System.out.println("\n\n" + user.getId() + file.getOriginalFilename() + "\n\n");
				Path path1 = Paths
						.get(savefFile.getAbsolutePath() + File.separator + user.getId() + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path1, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("path=" + path);
			} else {
				System.out.println("file not changed");
				user.setImageUrl(user1.getImageUrl());
			}
			user.setEnabled(user1.isEnabled());
			user.setPassword(user1.getPassword());
			StrongTextEncryptor ste = new StrongTextEncryptor();
			ste.setPassword((String) session.getAttribute("key"));
			user.setAbout(ste.encrypt(user1.getAbout()));
			user.setId(user1.getId());
			user.setRole(user1.getRole());
			System.out.println(user1);
			this.userRepository.save(user);

		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
		}
		return "redirect:/user/profile";
	}

	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data, HttpSession session) throws RazorpayException {
		String email = session.getAttribute("email").toString();
		int amt = Integer.parseInt(data.get("amount").toString());
		var client = new RazorpayClient("rzp_test_uV08rR1BPLKQA1", "9Wfqm1tVT98x8ffqroXxMCVp");
		JSONObject options = new JSONObject();
		options.put("amount", amt * 100);
		options.put("currency", "INR");
		options.put("receipt", "txn_1841012055");
		Order order = client.Orders.create(options);
		// save order
		MyOrder order1 = new MyOrder();
		order1.setAmount(String.valueOf(Integer.parseInt(order.get("amount").toString()) / 100));
		order1.setOrderId(order.get("id"));
		order1.setPaymentId(null);
		order1.setStatus("created");
		order1.setUser(this.userRepository.getUserByUserName(email));
		order1.setReceipt(order.get("receipt"));
		this.myOrderRepository.save(order1);
		return order.toString();
	}

	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data) {

		System.out.println(data + " = dsata");
		MyOrder myorder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		myorder.setPaymentId(data.get("payment_id").toString());
		myorder.setStatus(data.get("status").toString());
		this.myOrderRepository.save(myorder);
		return ResponseEntity.ok(Map.of("msg", "updated"));
	}

}
