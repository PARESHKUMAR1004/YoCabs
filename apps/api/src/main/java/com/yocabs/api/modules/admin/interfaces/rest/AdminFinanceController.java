package com.yocabs.api.modules.admin.interfaces.rest;

import com.yocabs.api.modules.admin.application.AdminFinanceService;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.CancellationsView;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.PaymentsView;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.RefundsView;
import com.yocabs.api.modules.payment.domain.model.PaymentStatus;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Money views for operations; date filters default to the last 30 days. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminFinanceController {

    private final AdminFinanceService financeService;

    public AdminFinanceController(AdminFinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/payments")
    public PaymentsView payments(
            @CurrentActor Actor actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return financeService.payments(actor, from, to, status, limit);
    }

    @GetMapping("/refunds")
    public RefundsView refunds(
            @CurrentActor Actor actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return financeService.refunds(actor, from, to, limit);
    }

    @GetMapping("/cancellations")
    public CancellationsView cancellations(
            @CurrentActor Actor actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return financeService.cancellations(actor, from, to, limit);
    }
}
