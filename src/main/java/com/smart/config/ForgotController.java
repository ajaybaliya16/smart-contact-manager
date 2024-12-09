package com.smart.config;
import java.util.Random;
import com.smart.service.emailService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ForgotController {
	
	Random random = new Random(1000);
	
	@Autowired
	private emailService emailService;
	
	@RequestMapping("/forgot")
	public String openEmailForm(){
		return "forgot_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam("email") String email, HttpSession session) {
		
		int otp = random.nextInt(9999);
		String subject = "OTP from SCM";
		String msg = "<h1>OTP = "+otp+"</h1>";
		String to = email;
		boolean flag = this.emailService.sendEmail(subject, msg, to);
		
		if(flag) {
			return "verify_otp";
		}else {
			session.setAttribute("message", "Check your email id!!");
			return "change_password";
		}
	}
}
