package com.fraktal.payment.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fraktal.payment.Payment;

@RestController
public class PaymentController {

    // This method is used to redirect the user to the payment page
    @GetMapping(value = "/goPaymentPage", produces = MediaType.TEXT_HTML_VALUE)
    public String goPaymentPage() throws Exception {
        Payment payment = new Payment();
        String pSign = payment.getPSign();
        String bankAPIURL = "https://testmpi.3dsecure.az/cgi-bin/cgi_link"; //Bank api URL

        String form = "<form ACTION=\""+ bankAPIURL +"\" METHOD=\"POST\" name='yavuz'>" +
        "<input name=\"AMOUNT\" value=\"" + payment.getAmount() + "\" type=\"hidden\">" +
        "<input name=\"CURRENCY\" value=\"" + payment.getCurrency() + "\" type=\"hidden\">" +
        "<input name=\"ORDER\" value=\"" + payment.getOrder() + "\" type=\"hidden\">" +
        "<input name=\"DESC\" value=\"" + payment.getDescription() + "\" type=\"hidden\">" +
        "<input name=\"MERCH_NAME\" value=\"" + payment.getMerchantName() + "\" type=\"hidden\">" +
        "<input name=\"MERCH_URL\" value=\"" + payment.getSuccessUrl() + "\" type=\"hidden\">" +
        "<input name=\"TERMINAL\" value=\"" + payment.getTerminal() + "\" type=\"hidden\">" +
        "<input name=\"EMAIL\" value=\"" + payment.getMerchantEmail() + "\" type=\"hidden\">" +
        "<input name=\"TRTYPE\" value=\"" + payment.getTrType() + "\" type=\"hidden\">" +
        "<input name=\"COUNTRY\" value=\"" + payment.getCountry() + "\" type=\"hidden\">" +
        "<input name=\"MERCH_GMT\" value=\"" + payment.getMerchGmt() + "\" type=\"hidden\">" +
        "<input name=\"BACKREF\" value=\"" + payment.getSuccessUrl() + "\" type=\"hidden\">" +
        "<input name=\"TIMESTAMP\" value=\"" + payment.getOperationTime() + "\" type=\"hidden\">" +
        "<input name=\"NONCE\" value=\"" + payment.getNonce() + "\" type=\"hidden\">" +
        "<input name=\"LANG\" value=\"AZ\" type=\"hidden\">" +
        "<input name=\"P_SIGN\" value=\"" + pSign + "\"  type=\"hidden\">" +
        "<input name=\"NAME\" value=\"" + payment.getName() + "\"  type=\"hidden\">" +
        "<input name=\"M_INFO\" value=\"" + payment.getMInfo() + "\"  type=\"hidden\">" + 
        "<input alt=\"Submit\" type=\"submit\" style='display: none'>" +
        "</form>" +
        "<script>window.onload = function() { document.forms['yavuz'].submit(); }</script>";

        //AMOUNT, CURRENCY, TERMINAL, TRTYPE, TIMESTAMP, NONCE, MERCH_URL

        System.out.println("\n\n"+form);

        return form;
    }
}