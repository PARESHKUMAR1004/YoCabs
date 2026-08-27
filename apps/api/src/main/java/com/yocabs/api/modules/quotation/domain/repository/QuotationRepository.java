package com.yocabs.api.modules.quotation.domain.repository;

import com.yocabs.api.modules.quotation.domain.model.Quotation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository {

    Quotation create(Quotation quotation);

    Quotation update(Quotation quotation);

    Optional<Quotation> findById(UUID id);

    List<Quotation> findByTripRequestId(UUID tripRequestId);

    List<Quotation> findByTravelPartnerId(UUID travelPartnerId);
}