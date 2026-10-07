package com.yocabs.api.modules.touristfeedback.interfaces.rest;

import com.yocabs.api.modules.touristfeedback.application.TouristFeedbackService;
import com.yocabs.api.modules.touristfeedback.domain.TouristFeedback;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
public class TouristFeedbackController {

    private final TouristFeedbackService touristFeedbackService;

    public TouristFeedbackController(TouristFeedbackService touristFeedbackService) {
        this.touristFeedbackService = touristFeedbackService;
    }

    @PostMapping("/api/v1/bookings/{bookingId}/tourist-feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public TouristFeedbackResponse submit(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId,
            @RequestBody SubmitFeedbackRequest request
    ) {
        if (request.rating() == null) {
            throw new IllegalArgumentException("Rating is required");
        }

        return TouristFeedbackResponse.from(
                touristFeedbackService.submit(actor, bookingId, request.rating(), request.comment())
        );
    }

    public record SubmitFeedbackRequest(Integer rating, String comment) {
    }

    public record TouristFeedbackResponse(
            UUID id,
            UUID bookingId,
            int rating,
            String comment,
            Instant createdAt
    ) {
        static TouristFeedbackResponse from(TouristFeedback feedback) {
            return new TouristFeedbackResponse(
                    feedback.id(), feedback.bookingId(), feedback.rating(),
                    feedback.comment(), feedback.createdAt()
            );
        }
    }
}
