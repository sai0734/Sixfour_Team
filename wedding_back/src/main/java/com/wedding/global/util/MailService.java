package com.wedding.global.util;

public interface MailService {

  void sendVerificationEmail(String toEmail, String token);

  void sendPasswordResetEmail(String toEmail, String token);

}