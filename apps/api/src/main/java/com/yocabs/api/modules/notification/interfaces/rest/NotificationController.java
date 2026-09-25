package com.yocabs.api.modules.notification.interfaces.rest;

import com.yocabs.api.modules.notification.application.NotificationService;
import com.yocabs.api.modules.notification.domain.model.Notification;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(
            @CurrentActor Actor actor,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return notificationService.list(actor, unreadOnly, limit).stream()
                .map(NotificationResponse::from).toList();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@CurrentActor Actor actor) {
        return new UnreadCountResponse(notificationService.unreadCount(actor));
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @CurrentActor Actor actor,
            @PathVariable UUID notificationId
    ) {
        notificationService.markRead(actor, notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@CurrentActor Actor actor) {
        notificationService.markAllRead(actor);
    }

    public record UnreadCountResponse(long unread) {
    }

    public record NotificationResponse(
            UUID id,
            String type,
            String title,
            String body,
            String referenceType,
            UUID referenceId,
            boolean read,
            Instant createdAt
    ) {

        static NotificationResponse from(Notification notification) {
            return new NotificationResponse(
                    notification.getId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getBody(),
                    notification.getReferenceType(),
                    notification.getReferenceId(),
                    notification.getReadAt() != null,
                    notification.getCreatedAt()
            );
        }
    }
}
