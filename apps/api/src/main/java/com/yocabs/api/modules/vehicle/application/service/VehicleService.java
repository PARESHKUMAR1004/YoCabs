package com.yocabs.api.modules.vehicle.application.service;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    private final TravelPartnerRepository
            travelPartnerRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            TravelPartnerRepository travelPartnerRepository
    ) {
        this.vehicleRepository =
                vehicleRepository;

        this.travelPartnerRepository =
                travelPartnerRepository;
    }

    @Transactional
    public Vehicle createVehicle(
            UUID travelPartnerId,
            String registrationNumber,
            String make,
            String model,
            VehicleCategory category,
            int passengerCapacity
    ) {

        TravelPartner travelPartner =
                getTravelPartner(
                        travelPartnerId
                );

        requireActivePartner(
                travelPartner
        );

        Vehicle vehicle =
                Vehicle.create(
                        travelPartnerId,
                        registrationNumber,
                        make,
                        model,
                        category,
                        passengerCapacity
                );

        return vehicleRepository.create(
                vehicle
        );
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicle(
            UUID travelPartnerId,
            UUID vehicleId
    ) {

        requireId(
                travelPartnerId,
                "Travel partner ID"
        );

        requireId(
                vehicleId,
                "Vehicle ID"
        );

        Vehicle vehicle =
                vehicleRepository
                        .findById(vehicleId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vehicle not found: "
                                                + vehicleId
                                )
                        );

        requireVehicleBelongsToPartner(
                vehicle,
                travelPartnerId
        );

        return vehicle;
    }

    @Transactional
    public Vehicle updateVehicle(
            UUID travelPartnerId,
            UUID vehicleId,
            String registrationNumber,
            String make,
            String model,
            VehicleCategory category,
            int passengerCapacity
    ) {

        TravelPartner travelPartner =
                getTravelPartner(travelPartnerId);

        requireActivePartner(travelPartner);

        Vehicle vehicle =
                getVehicle(
                        travelPartnerId,
                        vehicleId
                );

        /*
         * Vehicle currently has no general update method.
         * We should not mutate its private fields from the
         * application layer.
         *
         * The next step is therefore to add a domain method
         * to Vehicle for changing its editable details.
         */

        vehicle.updateDetails(
                registrationNumber,
                make,
                model,
                category,
                passengerCapacity
        );

        return vehicleRepository.update(
                vehicle
        );
    }



    @Transactional(readOnly = true)
    public List<Vehicle> listVehicles(
            UUID travelPartnerId
    ) {

        requireId(
                travelPartnerId,
                "Travel partner ID"
        );

        getTravelPartner(
                travelPartnerId
        );

        return vehicleRepository
                .findByTravelPartnerId(
                        travelPartnerId
                );
    }

    private TravelPartner getTravelPartner(
            UUID travelPartnerId
    ) {

        requireId(
                travelPartnerId,
                "Travel partner ID"
        );

        return travelPartnerRepository
                .findById(travelPartnerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Travel partner not found: "
                                        + travelPartnerId
                        )
                );
    }

    private void requireActivePartner(
            TravelPartner travelPartner
    ) {

        if (travelPartner.getStatus()
                != TravelPartnerStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Only an active travel partner can manage vehicles"
            );
        }
    }

    private void requireVehicleBelongsToPartner(
            Vehicle vehicle,
            UUID travelPartnerId
    ) {

        if (!vehicle.getTravelPartnerId()
                .equals(travelPartnerId)) {

            throw new IllegalArgumentException(
                    "Vehicle does not belong to travel partner: "
                            + travelPartnerId
            );
        }
    }

    private void requireId(
            UUID id,
            String fieldName
    ) {

        if (id == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }
    }


    @Transactional
    public Vehicle makeAvailable(
            UUID travelPartnerId,
            UUID vehicleId
    ) {

        TravelPartner travelPartner =
                getTravelPartner(travelPartnerId);

        requireActivePartner(travelPartner);

        Vehicle vehicle =
                getVehicle(
                        travelPartnerId,
                        vehicleId
                );

        vehicle.makeAvailable();

        return vehicleRepository.update(
                vehicle
        );
    }

    @Transactional
    public Vehicle makeUnavailable(
            UUID travelPartnerId,
            UUID vehicleId
    ) {

        TravelPartner travelPartner =
                getTravelPartner(travelPartnerId);

        requireActivePartner(travelPartner);

        Vehicle vehicle =
                getVehicle(
                        travelPartnerId,
                        vehicleId
                );

        vehicle.makeUnavailable();

        return vehicleRepository.update(
                vehicle
        );
    }

    @Transactional
    public Vehicle sendToMaintenance(
            UUID travelPartnerId,
            UUID vehicleId
    ) {

        TravelPartner travelPartner =
                getTravelPartner(travelPartnerId);

        requireActivePartner(travelPartner);

        Vehicle vehicle =
                getVehicle(
                        travelPartnerId,
                        vehicleId
                );

        vehicle.sendToMaintenance();

        return vehicleRepository.update(
                vehicle
        );
    }

    @Transactional
    public Vehicle deactivate(
            UUID travelPartnerId,
            UUID vehicleId
    ) {

        TravelPartner travelPartner =
                getTravelPartner(travelPartnerId);

        requireActivePartner(travelPartner);

        Vehicle vehicle =
                getVehicle(
                        travelPartnerId,
                        vehicleId
                );

        vehicle.deactivate();

        return vehicleRepository.update(
                vehicle
        );
    }
}