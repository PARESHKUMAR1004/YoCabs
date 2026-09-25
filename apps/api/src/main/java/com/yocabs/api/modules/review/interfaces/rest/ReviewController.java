package com.yocabs.api.modules.review.interfaces.rest;

import com.yocabs.api.modules.review.application.ReviewService;
import com.yocabs.api.modules.review.domain.Review;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/v1/bookings/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse submit(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId,
            @RequestBody SubmitReviewRequest request
    ) {
        if (request.rating() == null) {
            throw new IllegalArgumentException("Rating is required");
        }

        return ReviewResponse.from(
                reviewService.submit(actor, bookingId, request.rating(), request.comment())
        );
    }

    @GetMapping("/api/v1/travel-partners/{travelPartnerId}/reviews")
    public List<ReviewResponse> list(
            @PathVariable UUID travelPartnerId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return reviewService.listForPartner(travelPartnerId, limit).stream()
                .map(ReviewResponse::from).toList();
    }

    @GetMapping("/api/v1/travel-partners/{travelPartnerId}/rating")
    public RatingResponse rating(@PathVariable UUID travelPartnerId) {
        var summary = reviewService.summary(travelPartnerId);
        return new RatingResponse(summary.average(), summary.count());
    }

    public record SubmitReviewRequest(Integer rating, String comment) {
    }

    public record RatingResponse(double average, long reviewCount) {
    }

    public record ReviewResponse(
            UUID id,
            UUID bookingId,
            int rating,
            String comment,
            Instant createdAt
    ) {

        static ReviewResponse from(Review review) {
            return new ReviewResponse(
                    review.id(), review.bookingId(), review.rating(), review.comment(), review.createdAt()
            );
        }
    }
}
