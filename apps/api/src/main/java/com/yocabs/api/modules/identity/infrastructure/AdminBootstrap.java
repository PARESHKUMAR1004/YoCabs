package com.yocabs.api.modules.identity.infrastructure;

import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first super administrator from configuration
 * (yocabs.bootstrap-admin.email / .password). No default credentials exist.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${yocabs.bootstrap-admin.email:}") String email,
            @Value("${yocabs.bootstrap-admin.password:}") String password
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (email.isBlank() || password.isBlank()) {
            return;
        }

        if (password.length() < 12) {
            throw new IllegalStateException(
                    "yocabs.bootstrap-admin.password must be at least 12 characters"
            );
        }

        if (users.findByEmail(email.trim().toLowerCase()).isPresent()) {
            return;
        }

        users.save(
                UserAccount.newAdmin(
                        email, passwordEncoder.encode(password), "Super Admin", Role.SUPER_ADMIN
                )
        );

        log.info("Bootstrap super administrator created for {}", email);
    }
}
