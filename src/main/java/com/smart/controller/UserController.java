package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import com.razorpay.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository repository;

	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;


	// method for adding common for response
	@ModelAttribute
	public void addCommondata(Model model, Principal principal) {
		String username = principal.getName();
		User userByUserName = this.repository.getUserByUserName(username);
		model.addAttribute("user", userByUserName);
	}

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	@GetMapping("add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());

		return "normal/add_contact_form";
	}

	@PostMapping("/proccess-contact")
	public String proccessContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {

			String username = principal.getName();
			User userByUserName = this.repository.getUserByUserName(username);

			// proccessing and uploading file
			if (file.isEmpty()) {

				contact.setImageUrl("contact.png");

			} else {
				// upload file to folder and update into DB
				contact.setImageUrl(file.getOriginalFilename());
				File savefile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			}

			contact.setUser(userByUserName);

			userByUserName.getContacts().add(contact);
			this.repository.save(userByUserName);

			// message success....

			session.setAttribute("message", new Message("Your contact is added !! Add more..", "success"));

		} catch (Exception e) {
			System.out.println("Error" + e.getMessage());
			e.printStackTrace();

			// error message...
			session.setAttribute("message", new Message("Somthing went wrong !! Try again..", "danger"));
		}
		return "normal/add_contact_form";
	}

	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") int page, Model model, HttpSession session, Principal principal) {
		model.addAttribute("title", "View Contact");
		String username = principal.getName();
		User user = this.repository.getUserByUserName(username);

		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentpage", page);
		model.addAttribute("totalpages", contacts.getTotalPages());
		return "normal/show_contacts";
	}

	@GetMapping("/{cId}/contact")
	public String ShowContactDetail(@PathVariable("cId") int cid, Model model, Principal principal) {
		try {
			Optional<Contact> contactoptional = this.contactRepository.findById(cid);
			Contact contact = contactoptional.get();
			String username = principal.getName();
			User userByUserName = this.repository.getUserByUserName(username);

			if (userByUserName.getId() == contact.getUser().getId()) {
				model.addAttribute("title", contact.getName());
				model.addAttribute("contact", contact);
			}
			model.addAttribute("content_message", "You don't have permission to view this contact..");
			return "normal/contact_detail";
		} catch (Exception e) {
			model.addAttribute("content_message", "somthing went worng...");
			return "normal/contact_detail";
		}

	}

	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") int cid, Model model, HttpSession session, Principal principal) {
		Optional<Contact> contactoptional = this.contactRepository.findById(cid);
		Contact contact = contactoptional.get();

		String username = principal.getName();
		User userByUserName = this.repository.getUserByUserName(username);

		if (userByUserName.getId() == contact.getUser().getId()) {

			userByUserName.getContacts().remove(contact);
			this.repository.save(userByUserName);
			// contact.setUser(null);// first unlink and then delete
			session.setAttribute("message", new Message("contact deleted successfully", "success"));

			// this.contactRepository.delete(contact);
		}
		return "redirect:/user/show-contacts/0";
	}

	// open update form handler
	@PostMapping("/update/{cId}")
	public String updateForm(@PathVariable("cId") int cid, Model model) {
		model.addAttribute("title", "Update contact");
		Contact contact = this.contactRepository.findById(cid).get();
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}

	// update contact handler
	@PostMapping("/proccess-update-contact")
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model model, HttpSession session, Principal principal) {
		try {

			Contact oldcontactdetails = this.contactRepository.findById(contact.getcId()).get();
			if (!file.isEmpty()) {

				// file work

				// delete old photo
				File deletefile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deletefile, oldcontactdetails.getImageUrl());
				file1.delete();

				// update new photo
				File savefile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImageUrl(file.getOriginalFilename());

			} else {
				contact.setImageUrl(oldcontactdetails.getImageUrl());
			}
			User user = this.repository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new Message("Your contact is updated", "success"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user/" + contact.getcId() + "/contact";
	}

	// your profile
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}

	@GetMapping("/settings")
	public String openSetting() {
		return "normal/settings";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldpassword") String oldpssword,
			@RequestParam("newpassword") String newpassword, Principal principal, HttpSession session) {

		String username = principal.getName();
		User userByUserName = this.repository.getUserByUserName(username);
		if (this.bCryptPasswordEncoder.matches(oldpssword, userByUserName.getPassowrd())) {
			userByUserName.setPassowrd(this.bCryptPasswordEncoder.encode(newpassword));
			this.repository.save(userByUserName);
			session.setAttribute("message", new Message("Your password successfully changed", "success"));
			return "redirect:/user/index";
		} else {
			session.setAttribute("message", new Message("please enter your correct old password", "danger"));
			return "redirect:/user/settings";
		}

	}

	// creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data,Principal principal) throws RazorpayException {
		System.out.println(data);
		int amt = Integer.parseInt(data.get("amount").toString());

		var client = new RazorpayClient("rzp_test_v4fLZnccQkwZzP", "d5vWYksc18GHetOUgswkEYRO");
		JSONObject options = new JSONObject();
		options.put("amount", amt*100);
		options.put("currency", "INR");
		options.put("receipt", "txn_123456");
		
		Order order = client.Orders.create(options);
		System.out.println(order);
		
		//we can save this order to our database
		
		return order.toString();
	}

}
