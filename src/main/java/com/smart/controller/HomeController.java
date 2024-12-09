package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.service.emailService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {

	Random random = new Random(1000);

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private emailService emailService;

	@Autowired
	private UserRepository userRepository;

	@RequestMapping("/")
	public String Home(Model model) {
		model.addAttribute("title", "Home - Smart Contact Manager");
		return "home";
	}

	@RequestMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About - Smart Contact Manager");
		return "about";
	}

	@RequestMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "Register - Smart Contact Manager");
		model.addAttribute("user", new User());
		return "signup";
	}

	@PostMapping("/do_register")
	public String registerUser(@Valid @ModelAttribute User user, BindingResult result1,
			@RequestParam(value = "agreement", defaultValue = "false") boolean agreement, Model model,
			HttpSession session) {

		try {

			if (!agreement) {
				System.out.println("you have not agreed the teams and condition");
				throw new Exception("you have not agreed the teams and condition");
			}
			if (result1.hasErrors()) {
				model.addAttribute("user", user);

				return "signup";
			}
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("defualt.png");
			user.setPassowrd(passwordEncoder.encode(user.getPassowrd()));

			User result = this.userRepository.save(user);

			model.addAttribute("user", new User());
			session.setAttribute("message", new Message("Succesfully Registered.!! ", "alert-success"));

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			session.setAttribute("message", new Message("Somthing went wrong..!! " + e.getMessage(), "alert-danger"));
			return "signup";
		}
		// return "signup";
		return "redirect:/login";
	}

	@GetMapping("/login")
	public String CustomLogin(Model model) {
		model.addAttribute("title", "Login - Smart Contact Manager");
		return "login";
	}

	// forgot password
	@GetMapping("/forgot")
	public String openEMailForm() {
		return "forgot_email_form";
	}

	@PostMapping("/send_otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {

		
		String otp = "";
		int ranNo;
		for (int i = 0; i < 6; i++) {
			// Generate random digit within 0-9
			ranNo = new Random().nextInt(9);
			otp = otp.concat(Integer.toString(ranNo));
		}
		// Return the generated OTP
		
		
		System.out.println("otp is: "+otp);
		// otp send on email
		String subject = "OTP from smart contact manager";
		String messgage = "" + "<div style='border:1px solid #e2e2e2; padding :20px'>" + "<h1>" + "OTP is " + "<b>"
				+ otp + "</n>" + "</h1> " + "</div>";
		String to = email;
		boolean flag = this.emailService.sendEmail(subject, messgage, email);
		if (flag == true) {
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		} else {
			session.setAttribute("message", new Message("check your email id", "alert-danger"));
			return "redirect:/forgot_email_form";
		}

	}

	@PostMapping("/verify_otp")
	public String verifyOTP(@RequestParam("otp") String otp, HttpSession session) {
		String myotp = (String) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");

		if (myotp.equals(otp)) {
			User user = userRepository.getUserByUserName(email);
			if (user == null) {
				session.setAttribute("message", new Message("User don't exist with this email", "alert-danger"));
				return "redirect:/forgot_email_form";
			}

			return "password_changed_form";
		} else {
			session.setAttribute("message", new Message("you have enter wrong OTP..", "alert-danger"));
			return "verify_otp";
		}
	}

	@PostMapping("/change_password")
	public String changePassword(@RequestParam("changedPassword") String changedPassword, HttpSession session) {
		String email = (String) session.getAttribute("email");
		User user = this.userRepository.getUserByUserName(email);
		user.setPassowrd(passwordEncoder.encode(changedPassword));
		this.userRepository.save(user);

		session.setAttribute("message", new Message("Your password is change successfully..", "alert-success"));

		return "redirect:/login";
	}
}
