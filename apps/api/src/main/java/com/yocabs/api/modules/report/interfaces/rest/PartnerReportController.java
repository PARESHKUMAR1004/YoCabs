package com.yocabs.api.modules.report.interfaces.rest;

import com.yocabs.api.modules.report.application.PartnerReportService;
import com.yocabs.api.modules.report.application.PartnerReports.CancellationsReport;
import com.yocabs.api.modules.report.application.PartnerReports.DriverRow;
import com.yocabs.api.modules.report.application.PartnerReports.EarningsReport;
import com.yocabs.api.modules.report.application.PartnerReports.TripsReport;
import com.yocabs.api.modules.report.application.PartnerReports.VehicleRow;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Dates filter by the trip's start date; default range is the last 30 days. */
@RestController
@RequestMapping("/api/v1/travel-partners/{travelPartnerId}/reports")
public class PartnerReportController {

    private final PartnerReportService reportService;

    public PartnerReportController(PartnerReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/earnings")
    public EarningsReport earnings(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.earnings(actor, travelPartnerId, from, to);
    }

    @GetMapping("/trips")
    public TripsReport trips(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return reportService.trips(actor, travelPartnerId, from, to, limit);
    }

    @GetMapping("/vehicles")
    public List<VehicleRow> vehicles(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.vehicles(actor, travelPartnerId, from, to);
    }

    @GetMapping("/drivers")
    public List<DriverRow> drivers(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.drivers(actor, travelPartnerId, from, to);
    }

    @GetMapping("/cancellations")
    public CancellationsReport cancellations(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return reportService.cancellations(actor, travelPartnerId, from, to, limit);
    }
}
