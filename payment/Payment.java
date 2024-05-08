package com.fraktal.payment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import java.util.Base64;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;

public class Payment {

    private static final SecureRandom random = new SecureRandom();

    private static final String MERCHANT_NAME = "merchantName"; //Merchant name
    private static final String MERCHANT_EMAIL = "email@merchant.com"; //Merchant email
    private static final String TERMINAL = "11111111"; //Terminal number (8 characters)
    private static final String CURRENCY = "AZN"; //Currency
    private static final String MERCH_GMT = "+4"; //Merchant GMT
    private static final String COUNTRY = "AZ"; //Country

    private static final String privateKeyPath = "merchantPrivateKeyPath.pem"; //Path to Merchant Private Key for signing request
    private String bankPublicKeyPath = "bankPublicKey.pem"; //Path to Bank Public Key for verifying response
    private String publicKeyPath = "merchantPublicKey.pem"; //Path to Merchant Public Key for verifying own PSign
    private static final String successUrl = "successUrl"; //URL to redirect customer after successful payment (https://www.website.com/success)
    private String order = "11111111"; //Order number (unique for each transaction)
    private static final String description = "Test Description"; //Description of the transaction
    private double amount = 10.00; //Amount of the transaction
    private static final int trType = 1; //Transaction type (1 - Purchase, 21 - Payment, 22 - Refund) -> https://developer.azericard.com/
    private String operationTime = "20240501135400"; //Operation time (YYYYMMDDHHMMSS)
    private String nonce = "487f7b22f68312d2"; //Nonce (Ramdom 16 characters)
    private String pSign = ""; //PSign (512 characters)
    private String pSignGenerated = ""; //Generated PSign (512 characters) for testing purposes
    private String name = "Yavuz Selim Öztürk"; //Customer name
    private String mInfo = "{\r\n" + // for details and configuration -> https://developer.azericard.com/
                "    \"browserScreenHeight\": \"1920\",\r\n" + //
                "    \"browserScreenWidth\": \"1080\",\r\n" + //
                "    \"browserTZ\": \"0\",\r\n" + //
                "    \"mobilePhone\": {\r\n" + //
                "        \"cc\": \"994\",\r\n" + //
                "        \"subscriber\": \"5077777777\"\r\n" + //
                "    }\r\n" + //
                "}";

    private String encodedMinfo = ""; //Encoded mInfo for the request form
   
    public String signBody= ""; //SignBody for PSign generation

    
    public Payment() throws Exception{
        //Creates current time GMT+0 
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        this.setOperationTime(now.format(formatter));
        //Creates random nonce
        this.setNonce(this.generateNonce(16));
        //Creates random amount
        DecimalFormat dfAmount = new DecimalFormat("#.##");
        double randomAmount = this.getAmount() + java.lang.Math.random() * 100;
        randomAmount = 98.44;
        this.setAmount(Double.parseDouble(dfAmount.format(randomAmount)));
        //Creates random order number
        DecimalFormat dfOrder = new DecimalFormat("#");
        double randomOrder = Double.parseDouble(this.getOrder()) + java.lang.Math.random() * 100000;
        this.setOrder(dfOrder.format(randomOrder));

        //Creates signBody for MAC (PSign) creation
        StringBuilder data = new StringBuilder("");
        data.append(String.valueOf(this.getAmount()).length()).append(String.valueOf(this.getAmount()))//AMOUNT
        .append(this.CURRENCY.length()).append(this.CURRENCY)//CURRENCY
        .append(this.TERMINAL.length()).append(this.TERMINAL)//TERMINAL
        .append(String.valueOf(this.trType).length()).append(String.valueOf(this.trType))//TRTYPE
        .append(this.getOperationTime().length()).append(this.getOperationTime())//TIMESTAMP
        .append(this.getNonce().length()).append(this.getNonce())//NONCE
        .append(this.successUrl.length()).append(this.successUrl);//MERCH_URL
        signBody = data.toString();

        //Generates PSign using signBody and private key
        this.pSignGenerated = this.generatePSign(signBody, privateKeyPath);
        this.pSign = this.pSignGenerated;
        //Compares generated PSign with PSign. This is for testing purposes!
        if (this.pSign.equals(this.pSignGenerated)){
            System.out.println("PSigns are equal");
        }else{
            System.out.println("PSigns are not equal");
        }

        //Encodes mInfo to Base64 (Required field in the request form)
        this.encodedMinfo = Base64.getEncoder().encodeToString(mInfo.getBytes(StandardCharsets.UTF_8));

    }

    //Method to fix public key file if inconsistent newline character are present 
    //\r windows, \n linux. These two can be mixed in the same file if the file is edited in different environments
    public void fixPublicKeyFile(String publicKeyPath) throws IOException {
    Path path = Paths.get(publicKeyPath);
    String content = new String(Files.readAllBytes(path));
    content = content.replace("\r\n", "\n");
    Files.write(path, content.getBytes());
    }

    //Primary method to verify PSign
    public Boolean verify(String pSign, Double amount, String terminal, String approval, String rrn, String intRef) throws IOException{

        String signBody = (amount == null ? "-" : String.valueOf(amount).length() + String.valueOf(amount)) + 
        (terminal == null ? "-" : terminal.length()+terminal) + 
        (approval == null ? "-" : approval.length()+approval) + 
        (rrn == null ? "-" : rrn.length()+rrn) + 
        (intRef == null ? "-" : intRef.length()+intRef);

        fixPublicKeyFile(this.getBankPublicKeyPath());
        System.out.println("Public key :" + this.getBankPublicKeyPath());
        System.out.println("Sign Body: " + signBody);
        
        Boolean isSignatureValid = false;

        try{
            PublicKey publicKey = loadPublicKey(this.getBankPublicKeyPath()); 

            Signature publicSignature = Signature.getInstance("SHA256withRSA");
            
            publicSignature.initVerify(publicKey);

            byte[] bytes = signBody.getBytes(StandardCharsets.UTF_8);
        
            publicSignature.update(bytes);
        
            byte[] signatureBytes = hexToBytes(pSign);
        
            isSignatureValid = publicSignature.verify(signatureBytes);
        
            //Second verify method is being called for test purposes. This method uses public key in PEM format
            //This is for testing purposes only
            System.out.println("Is signature valid: " + verifyPSign(signBody, pSign, this.getBankPublicKeyPath()));
        }catch (Exception e) {
            System.out.println("Error occurred while decrypting: " + e.toString());
        return false;
        }
        return isSignatureValid;
    }

    //Alternative method to load public key
    //This method uses PEMParser to load public key
    public PublicKey loadPublicKeyPemParser(String publicKeyPath) throws Exception {
        PEMParser pemParser = new PEMParser(new FileReader(publicKeyPath));
        SubjectPublicKeyInfo subjectPublicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
        pemParser.close();
        PublicKey publicKey = new JcaPEMKeyConverter().getPublicKey(subjectPublicKeyInfo);
        return publicKey;
    }

    //Primary method for load Public Key from PEM file
    //This method uses PemReader to load public key
    private PublicKey loadPublicKey(String publicKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    PemObject pemObject;
    try (PemReader pemReader = new PemReader(new FileReader(publicKeyPath))) {
        pemObject = pemReader.readPemObject();
    }
        byte[] publicKeyBytes = pemObject.getContent();
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    //Method to convert hex string to byte array
    //This method is used to convert hex string to byte array for signature verification
    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
        }
        return bytes;
    }

    //Load Private Key from PEM file
    //This method is used to load private key from PEM file
    //This method is used for PSign generation
    private PrivateKey loadKey(String privateKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemObject pemObject;
        try (PemReader pemReader = new PemReader(new FileReader(privateKeyPath))) {
            pemObject = pemReader.readPemObject();
        }

        byte[] pemContent = pemObject.getContent();
        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(pemContent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");    

        return keyFactory.generatePrivate(encodedKeySpec);
    }

    //PSign generation with Private Key
    //This method is used to generate PSign using private key
    public String generatePSign(String signBody, String privateKeyPath){
        String newPSign = "";
        try{ 
            PrivateKey privateKey = loadKey(privateKeyPath);
        
            Signature signature = Signature.getInstance("SHA256withRSA");
        
            signature.initSign(privateKey);
        
            byte[] bytes = signBody.getBytes(StandardCharsets.UTF_8);
        
            signature.update(bytes);
        
            byte[] signed = signature.sign();
        
            StringBuilder hex = new StringBuilder();
        
            for (var _byte : signed) {
                hex.append(String.format("%02x", _byte));
            }
            newPSign = hex.toString();

            //Verification of generated PSign using the pair public key of the private key which used to generate PSign
            //This is for testing purposes only
            System.out.println("Verification result: " + verifyPSign(signBody, newPSign, this.publicKeyPath));

        }catch (Exception e){
            System.out.println("PSign generation error occurred" + e.toString());
        }
        return newPSign;
        }

        //PSign verification with Public Key
        //This method is used to verify PSign using public key
        public boolean verifyPSign(String signBody, String pSign, String publicKeyPath){
            try{
                System.out.println("Input params: " + signBody + " " + pSign + " " + publicKeyPath);
                PublicKey publicKey = loadPublicKeyForGenerated(publicKeyPath); 
        
                Signature publicSignature = Signature.getInstance("SHA256withRSA");
            
                publicSignature.initVerify(publicKey);
        
                byte[] bytes = signBody.getBytes(StandardCharsets.UTF_8);
            
                publicSignature.update(bytes);
            
                byte[] signatureBytes = hexToBytes(pSign);
            
                return publicSignature.verify(signatureBytes);
            } catch (Exception e) {
                System.out.println("Error occurred while verifying signature: " + e.toString());
                return false;
            }
        }

        //Load Public Key alternative method
        //This method is used to load public key from PEM file
        //This method deconstructs the public key and generates public key from it
        private PublicKey loadPublicKeyForGenerated(String publicKeyPath) throws Exception {
            String publicKeyContent = new String(Files.readAllBytes(Paths.get(publicKeyPath)))
                .replaceAll("\\n", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");
        
            KeyFactory kf = KeyFactory.getInstance("RSA");
        
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
            PublicKey pubKey = kf.generatePublic(keySpecX509);
            return pubKey;
        }
        
        //Nonce generation
        public static String generateNonce(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(Integer.toHexString(random.nextInt(16)));
            }
            return sb.toString();
        }
        
    
    public String toString(){
        return "Payment{" +
                "amount=" + amount +
                ", currency='" + CURRENCY + '\'' +
                ", order='" + order + '\'' +
                ", description='" + description + '\'' +
                ", merchantName='" + MERCHANT_NAME + '\'' +
                ", successUrl='" + successUrl + '\'' +
                ", terminal='" + TERMINAL + '\'' +
                ", merchantEmail='" + MERCHANT_EMAIL + '\'' +
                ", trType=" + trType +
                ", country='" + COUNTRY + '\'' +
                ", merchGmt='" + MERCH_GMT + '\'' +
                ", operationTime='" + operationTime + '\'' +
                ", nonce='" + nonce + '\'' +
                ", pSign='" + pSign + '\'' +
                ", name='" + name + '\'' +
                ", mInfo='" + encodedMinfo + '\'' +
                '}';
    }

    public String getName(){
        return this.name;
    }

    public String getMInfo(){
        return this.encodedMinfo;
    }

    public String getPSign(){
        return this.pSign;
    }

    public String getAmountToString(){
        return String.valueOf(this.amount);
    }

    public double getAmount(){
        return this.amount;
    }
    
    public void setAmount(double amount){
        this.amount = amount;
    }

    public String getCurrency(){
        return this.CURRENCY;
    }

    public String getOrder(){
        return this.order;
    }

    public void setOrder(String order){
        this.order = order;
    }

    public String getDescription(){
        return this.description;
    }

    public String getMerchantName(){
        return this.MERCHANT_NAME;
    }

    public String getSuccessUrl(){
        return this.successUrl;
    }

    public String getTerminal(){
        return this.TERMINAL;
    }

    public static String getMerchantEmail(){
        return MERCHANT_EMAIL;
    }

    public static int getTrType(){
        return trType;
    }

    public static String getCountry(){
        return COUNTRY;
    }

    public static String getMerchGmt(){
        return MERCH_GMT;
    }

    public String getOperationTime(){
        return operationTime;
    }

    public void setOperationTime(String operationTime){
        this.operationTime = operationTime;
    }

    public String getNonce(){
        return nonce;
    }

    public void setNonce(String nonce){
        this.nonce = nonce;
    }

    public String getBankPublicKeyPath(){
        return this.bankPublicKeyPath;
    }

    public void setBankPublicKeyPath(String bankPublicKeyPath){
        this.bankPublicKeyPath = bankPublicKeyPath;
    }
}
