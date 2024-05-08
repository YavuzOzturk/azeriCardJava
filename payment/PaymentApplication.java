package com.fraktal.payment;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(PaymentApplication.class, args);

		String pSign=""; //PSign to be verified - 512 characters
		Double amount = 10.00;	//Amount to be verified
        String terminal = "11111111";	//Terminal to be verified - 8 characters
        String approval = "111111";	//Approval to be verified - 6 characters
        String rrn = "111111111111"; //RRN to be verified - 12 characters
        String intRef = "1111111111111111"; //Internal Reference to be verified - 16-32 characters

		Payment nPayment = new Payment();
		System.out.println(nPayment.verify(pSign, amount, terminal, approval, rrn, intRef));
	}

}
