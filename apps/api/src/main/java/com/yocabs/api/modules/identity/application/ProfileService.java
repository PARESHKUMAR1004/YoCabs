package com.yocabs.api.modules.identity.application;

import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The signed-in user's own profile (any role). */
@Service
public class ProfileService {

    private final UserAccountRepository users;

    public ProfileService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserAccount get(Actor actor) {
        return users.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    /** Only supplied fields change; an empty email clears it (non-admins). */
    @Transactional
    public UserAccount update(Actor actor, String displayName, String email, String preferredLanguage) {

        UserAccount account = get(actor);

        account.updateProfile(displayName, email, preferredLanguage);

        if (account.getEmail() != null) {
            users.findByEmail(account.getEmail())
                    .filter(other -> !other.getId().equals(account.getId()))
                    .ifPresent(other -> {
                        throw new IllegalStateException("This email is already in use");
                    });
        }

        return users.save(account);
    }
}
