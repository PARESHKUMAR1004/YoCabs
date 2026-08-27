package com.yocabs.api.modules.triprequest.application.service;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SubmitTripRequestService {

    private final TripRequestRepository tripRequestRepository;

    public SubmitTripRequestService(
            TripRequestRepository tripRequestRepository
    ) {
        this.tripRequestRepository = tripRequestRepository;
    }

    @Transactional
    public TripRequest execute(UUID id) {

        TripRequest tripRequest =
                tripRequestRepository
                        .findById(new TripRequestId(id))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Trip request not found: " + id
                                )
                        );

        tripRequest.submit();

        return tripRequestRepository.update(
                tripRequest
        );
    }
}