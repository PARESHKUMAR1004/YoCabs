package com.yocabs.api.modules.invoice.application;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.payment.domain.model.Payment;
import com.yocabs.api.modules.payment.domain.model.PaymentPurpose;
import com.yocabs.api.modules.payment.domain.model.PaymentStatus;
import com.yocabs.api.modules.pricing.domain.PriceComponent;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renders a completed trip as a one-page PDF bill: the fare breakdown exactly as it was priced,
 * what was paid and when, and the balance. No totals are recomputed here; every figure is read
 * straight off the booking and its payments, so the bill can never disagree with what was charged.
 */
@Component
public class InvoicePdfRenderer {

    private static final Color INK = new Color(0x0B, 0x12, 0x20);
    private static final Color GOLD = new Color(0xB0, 0x8D, 0x57);
    private static final Color MUTED = new Color(0x6B, 0x65, 0x59);
    private static final Color RULE = new Color(0xE1, 0xD3, 0xB4);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public byte[] render(
            Booking booking,
            String partnerName,
            String vehicleLabel,
            String touristName,
            List<Payment> payments
    ) {
        Document document = new Document(PageSize.A4, 48, 48, 56, 48);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            header(document);
            billTo(document, booking, touristName);
            tripDetails(document, booking, partnerName, vehicleLabel);
            fareTable(document, booking);
            paymentsTable(document, booking, payments);
            footer(document);

            document.close();
            return out.toByteArray();

        } catch (DocumentException exception) {
            throw new IllegalStateException("Could not render the invoice", exception);
        }
    }

    private void header(Document document) throws DocumentException {
        Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, GOLD);
        Font tagline = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED);

        Paragraph title = new Paragraph("YoCabs", brand);
        document.add(title);

        Paragraph subtitle = new Paragraph("Trip invoice", tagline);
        subtitle.setSpacingAfter(16);
        document.add(subtitle);

        addRule(document);
    }

    private void billTo(Document document, Booking booking, String touristName) throws DocumentException {
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INK);
        Font value = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);

        PdfPTable table = borderlessTable(2);

        table.addCell(cell("Booking reference", label));
        table.addCell(cell(booking.getId().toString(), value));
        table.addCell(cell("Billed to", label));
        table.addCell(cell(touristName == null ? "Traveller" : touristName, value));
        table.addCell(cell("Trip date", label));
        table.addCell(cell(formatRange(booking), value));

        table.setSpacingBefore(12);
        table.setSpacingAfter(16);
        document.add(table);
    }

    private void tripDetails(
            Document document,
            Booking booking,
            String partnerName,
            String vehicleLabel
    ) throws DocumentException {
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INK);
        Font value = FontFactory.getFont(FontFactory.HELVETICA, 11, INK);

        document.add(new Paragraph("Trip", heading));

        Paragraph route = new Paragraph();
        route.add(new Chunk(booking.getPickupDescription() + "  →  " + booking.getDestinationDescription(), value));
        route.setSpacingBefore(4);
        document.add(route);

        Paragraph partner = new Paragraph(
                "Operated by " + (partnerName == null ? "-" : partnerName)
                        + (vehicleLabel == null ? "" : " · " + vehicleLabel),
                FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED)
        );
        partner.setSpacingAfter(16);
        document.add(partner);
    }

    private void fareTable(Document document, Booking booking) throws DocumentException {
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INK);
        Font label = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
        Font amount = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
        Font totalLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, INK);
        Font totalAmount = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, GOLD.darker());

        document.add(new Paragraph("Fare", heading));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {3, 1});
        table.setSpacingBefore(8);

        for (PriceComponent component : booking.getPriceComponents()) {
            table.addCell(borderlessCell(component.description(), label, Element.ALIGN_LEFT));
            table.addCell(borderlessCell(money(component.amount(), booking.getCurrency()), amount, Element.ALIGN_RIGHT));
        }

        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setColspan(2);
        ruleCell.setBorder(PdfPCell.TOP);
        ruleCell.setBorderColor(RULE);
        ruleCell.setFixedHeight(8);
        table.addCell(ruleCell);

        table.addCell(borderlessCell("Total fare", totalLabel, Element.ALIGN_LEFT));
        table.addCell(borderlessCell(money(booking.getTotalAmount(), booking.getCurrency()), totalAmount, Element.ALIGN_RIGHT));

        table.setSpacingAfter(16);
        document.add(table);
    }

    private void paymentsTable(
            Document document,
            Booking booking,
            List<Payment> payments
    ) throws DocumentException {
        Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INK);
        Font label = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
        Font amount = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
        Font muted = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);

        document.add(new Paragraph("Payments", heading));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {3, 2, 2});
        table.setSpacingBefore(8);

        boolean anySucceeded = false;

        for (Payment payment : payments) {
            if (payment.getStatus() != PaymentStatus.SUCCEEDED
                    && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED
                    && payment.getStatus() != PaymentStatus.REFUNDED) {
                continue;
            }
            anySucceeded = true;

            String purpose = payment.getPurpose() == PaymentPurpose.TOKEN ? "Booking token" : "Trip balance";
            table.addCell(borderlessCell(purpose, label, Element.ALIGN_LEFT));
            table.addCell(borderlessCell(payment.getCreatedAt().atZone(ZoneId.of("Asia/Kolkata")).format(DATE_FORMAT), muted, Element.ALIGN_LEFT));
            table.addCell(borderlessCell(money(payment.getAmount(), payment.getCurrency()), amount, Element.ALIGN_RIGHT));
        }

        BigDecimal balance = booking.getTotalAmount().subtract(booking.getTokenAmount());
        BigDecimal paid = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED
                        || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
                        || payment.getStatus() == PaymentStatus.REFUNDED)
                .filter(payment -> payment.getPurpose() == PaymentPurpose.BALANCE)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!anySucceeded) {
            table.addCell(borderlessCell("No payment recorded", muted, Element.ALIGN_LEFT));
            PdfPCell blank = new PdfPCell();
            blank.setColspan(2);
            blank.setBorder(PdfPCell.NO_BORDER);
            table.addCell(blank);
        }

        document.add(table);

        Paragraph outstanding = new Paragraph(
                paid.compareTo(balance) >= 0
                        ? "Paid in full."
                        : "Balance of " + money(balance.subtract(paid), booking.getCurrency())
                                + " settled directly with the travel partner, unless paid in the app.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, MUTED)
        );
        outstanding.setSpacingBefore(8);
        outstanding.setSpacingAfter(16);
        document.add(outstanding);
    }

    private void footer(Document document) throws DocumentException {
        addRule(document);
        Paragraph note = new Paragraph(
                "This is a system-generated bill for a YoCabs trip. For questions, contact YoCabs support "
                        + "with the booking reference above.",
                FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED)
        );
        note.setSpacingBefore(8);
        document.add(note);
    }

    private static void addRule(Document document) throws DocumentException {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setBorderColor(RULE);
        cell.setBorderWidth(1.2f);
        cell.setFixedHeight(2);
        rule.addCell(cell);
        document.add(rule);
    }

    private static PdfPTable borderlessTable(int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        return table;
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingBottom(4);
        return cell;
    }

    private static PdfPCell borderlessCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPaddingBottom(6);
        return cell;
    }

    private static String formatRange(Booking booking) {
        String start = booking.getStartDate().format(DATE_FORMAT);
        String end = booking.getEndDate().format(DATE_FORMAT);
        return start.equals(end) ? start : start + " - " + end;
    }

    private static String money(BigDecimal amount, String currency) {
        String symbol = "INR".equals(currency) ? "Rs. " : currency + " ";
        return symbol + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
