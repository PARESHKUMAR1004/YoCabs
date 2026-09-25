package com.yocabs.api.modules.driver.application;

import com.yocabs.api.modules.driver.domain.model.Driver;
import com.yocabs.api.modules.driver.domain.repository.DriverRepository;
import com.yocabs.api.modules.identity.application.AuthService;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DriverService {

    private final DriverRepository drivers;
    private final UserAccountRepository users;
    private final TravelPartnerRepository partners;

    public DriverService(
            DriverRepository drivers,
            UserAccountRepository users,
            TravelPartnerRepository partners
    ) {
        this.drivers = drivers;
        this.users = users;
        this.partners = partners;
    }

    @Transactional
    public Driver addDriver(
            Actor actor,
            UUID travelPartnerId,
            String name,
            String rawMobile,
            String licenseNumber
    ) {
        actor.requirePartnerAccess(travelPartnerId);
        requireActivePartner(travelPartnerId);

        String mobile = AuthService.normalizeMobile(rawMobile);

        if (users.findByMobile(mobile).isPresent()) {
            throw new IllegalStateException("This mobile number is already registered");
        }

        UserAccount account =
                users.save(UserAccount.newDriver(mobile, name, travelPartnerId));

        return drivers.create(
                Driver.create(account.getId(), travelPartnerId, name, mobile, licenseNumber)
        );
    }

    @Transactional(readOnly = true)
    public List<Driver> list(Actor actor, UUID travelPartnerId) {
        actor.requirePartnerAccess(travelPartnerId);
        return drivers.findByTravelPartnerId(travelPartnerId);
    }

    @Transactional(readOnly = true)
    public Driver get(Actor actor, UUID travelPartnerId, UUID driverId) {
        actor.requirePartnerAccess(travelPartnerId);
        return load(travelPartnerId, driverId);
    }

    @Transactional
    public Driver deactivate(Actor actor, UUID travelPartnerId, UUID driverId) {

        actor.requirePartnerAccess(travelPartnerId);
        Driver driver = load(travelPartnerId, driverId);

        driver.deactivate();

        // An inactive driver must not be able to sign in.
        users.findById(driverId).ifPresent(account -> {
            account.block();
            users.save(account);
        });

        return drivers.update(driver);
    }

    @Transactional
    public Driver activate(Actor actor, UUID travelPartnerId, UUID driverId) {

        actor.requirePartnerAccess(travelPartnerId);
        requireActivePartner(travelPartnerId);
        Driver driver = load(travelPartnerId, driverId);

        driver.activate();

        users.findById(driverId).ifPresent(account -> {
            account.unblock();
            users.save(account);
        });

        return drivers.update(driver);
    }

    private Driver load(UUID travelPartnerId, UUID driverId) {

        Driver driver =
                drivers.findById(driverId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Driver not found: " + driverId));

        if (!driver.getTravelPartnerId().equals(travelPartnerId)) {
            throw new ResourceNotFoundException("Driver not found: " + driverId);
        }

        return driver;
    }

    private void requireActivePartner(UUID travelPartnerId) {

        TravelPartner partner =
                partners.findById(travelPartnerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Travel partner not found: " + travelPartnerId));

        if (partner.getStatus() != TravelPartnerStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only an active travel partner can manage drivers"
            );
        }
    }
}
