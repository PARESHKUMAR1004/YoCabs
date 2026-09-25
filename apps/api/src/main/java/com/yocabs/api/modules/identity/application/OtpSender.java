package com.yocabs.api.modules.identity.application;

/** Port for delivering one-time codes (SMS/WhatsApp provider adapters implement this). */
public interface OtpSender {

    void send(String mobile, String code);
}
