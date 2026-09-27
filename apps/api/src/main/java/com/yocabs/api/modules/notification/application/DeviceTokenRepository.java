package com.yocabs.api.modules.notification.application;

import java.util.List;
import java.util.UUID;

/** The phones a user has signed in on, as push tokens. */
public interface DeviceTokenRepository {

    /** Records the token for this user, moving it if another user had it on the same phone. */
    void register(UUID userId, String token, String platform);

    /** Forgets the token, but only if it belongs to this user. */
    void unregister(UUID userId, String token);

    /** Forgets a token the push service says is dead, whoever owned it. */
    void discard(String token);

    List<String> findTokens(UUID userId);
}
