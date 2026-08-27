package com.yocabs.api.modules.triprequest.application.service;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class GetTripRequestService {

    private final TripRequestRepository tripRequestRepository;

    public GetTripRequestService(
            TripRequestRepository tripRequestRepository
    ) {
        this.tripRequestRepository = tripRequestRepository;
    }

    @Transactional(readOnly = true)
    public TripRequest execute(UUID id) {

        return tripRequestRepository
                .findById(new TripRequestId(id))
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Trip request not found: " + id
                        )
                );
    }
}