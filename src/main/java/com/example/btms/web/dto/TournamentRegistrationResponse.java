package com.example.btms.web.dto;

/**
 * DTO for Tournament Registration Response
 * Sent back to client after successful registration
 * 
 * @author BTMS Team
 * @version 1.0
 */
public class TournamentRegistrationResponse {
    
    private boolean success;
    private String message;
    private Integer registrationId;
    private String registrationCode;
    private String confirmationEmail;
    private String paymentInstructions;
    
    // Constructors
    public TournamentRegistrationResponse() {
    }
    
    public TournamentRegistrationResponse(boolean success, String message, Integer registrationId, 
                                        String registrationCode, String confirmationEmail, 
                                        String paymentInstructions) {
        this.success = success;
        this.message = message;
        this.registrationId = registrationId;
        this.registrationCode = registrationCode;
        this.confirmationEmail = confirmationEmail;
        this.paymentInstructions = paymentInstructions;
    }
    
    // Factory methods
    public static TournamentRegistrationResponse success(Integer registrationId, 
                                                        String registrationCode, 
                                                        String email) {
        return new TournamentRegistrationResponse(
            true,
            "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận.",
            registrationId,
            registrationCode,
            email,
            null
        );
    }
    
    public static TournamentRegistrationResponse successWithPayment(Integer registrationId, 
                                                                   String registrationCode, 
                                                                   String email,
                                                                   String paymentInstructions) {
        return new TournamentRegistrationResponse(
            true,
            "Đăng ký thành công! Vui lòng hoàn tất thanh toán để xác nhận đăng ký.",
            registrationId,
            registrationCode,
            email,
            paymentInstructions
        );
    }
    
    public static TournamentRegistrationResponse error(String message) {
        return new TournamentRegistrationResponse(
            false,
            message,
            null,
            null,
            null,
            null
        );
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getRegistrationId() {
        return registrationId;
    }
    
    public void setRegistrationId(Integer registrationId) {
        this.registrationId = registrationId;
    }
    
    public String getRegistrationCode() {
        return registrationCode;
    }
    
    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }
    
    public String getConfirmationEmail() {
        return confirmationEmail;
    }
    
    public void setConfirmationEmail(String confirmationEmail) {
        this.confirmationEmail = confirmationEmail;
    }
    
    public String getPaymentInstructions() {
        return paymentInstructions;
    }
    
    public void setPaymentInstructions(String paymentInstructions) {
        this.paymentInstructions = paymentInstructions;
    }
}
