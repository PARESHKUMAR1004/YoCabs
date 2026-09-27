package com.yocabs.api.modules.notification.interfaces.rest;

import com.yocabs.api.modules.notification.application.DeviceTokenRepository;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A phone tells the API where to send this user's notifications, and stops when they sign out. */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private static final int MAX_TOKEN_LENGTH = 200;

    private final DeviceTokenRepository devices;

    public DeviceController(DeviceTokenRepository devices) {
        this.devices = devices;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@CurrentActor Actor actor, @RequestBody RegisterDeviceRequest request) {

        String token = request.token() == null ? "" : request.token().trim();

        // Only Expo push tokens are used, so anything else is a mistake worth refusing early.
        if (token.isEmpty()
                || token.length() > MAX_TOKEN_LENGTH
                || !(token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["))) {
            throw new IllegalArgumentException("That is not a valid push token");
        }

        String platform = request.platform() == null || request.platform().isBlank()
                ? "android"
                : request.platform().trim().toLowerCase();

        devices.register(actor.userId(), token, platform.length() > 20 ? platform.substring(0, 20) : platform);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@CurrentActor Actor actor, @RequestParam String token) {
        devices.unregister(actor.userId(), token);
    }

    public record RegisterDeviceRequest(String token, String platform) {
    }
}
