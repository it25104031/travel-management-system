package com.travel.module2_visa.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.travel.module2_visa.domain.DepartureStatus;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.domain.TourDepartureEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ManifestService {

    private static final String[] HEADERS = {"No", "Surname", "Given Names", "Gender", "Date of Birth", "Nationality",
            "Passport No", "Passport Expiry", "Visa Status", "Gov Reference", "Entry Type", "Visa Valid To"};

    private final DepartureService departureService;

    public ManifestService(DepartureService departureService) {
        this.departureService = departureService;
    }

    @Transactional(readOnly = true)
    public byte[] csv(Long departureId) {
        TourDepartureEntity departure = departureService.get(departureId);
        StringBuilder sb = new StringBuilder();
        sb.append("# Departure manifest,").append(csv(departure.getCode())).append(',').append(csv(departure.getTourName()))
                .append(",Departs ").append(departure.getDepartureDate()).append(",Returns ").append(departure.getReturnDate())
                .append(",Status ").append(departure.getStatus()).append('\n');
        sb.append(String.join(",", HEADERS)).append('\n');
        List<String[]> rows = rows(departure);
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(csv(row[i]));
            }
            sb.append('\n');
        }

        return ("﻿" + sb).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Long departureId) {
        TourDepartureEntity departure = departureService.get(departureId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        PdfWriter.getInstance(document, out);
        document.open();

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(23, 96, 147));
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        document.add(new Paragraph("Passenger Departure Manifest", title));
        document.add(new Paragraph(departure.getCode() + "  |  " + departure.getTourName()
                + "  |  Departs " + departure.getDepartureDate() + "  |  Returns " + departure.getReturnDate(), normal));
        String status = departure.getStatusEnum() == DepartureStatus.PENDING
                ? "PROVISIONAL: departure not yet cleared (booking gate locked)"
                : "Status: " + departure.getStatus();
        document.add(new Paragraph(status + "   Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normal));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 9, 11, 5, 7, 8, 8, 7, 7, 9, 6, 7});
        for (String h : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(h, header));
            cell.setBackgroundColor(new Color(23, 96, 147));
            cell.setPadding(4);
            table.addCell(cell);
        }
        for (String[] row : rows(departure)) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value, normal));
                cell.setPadding(3);
                table.addCell(cell);
            }
        }
        document.add(table);
        document.add(new Paragraph("Total passengers: " + departure.getTravellers().size(), normal));
        document.close();
        return out.toByteArray();
    }

    private List<String[]> rows(TourDepartureEntity departure) {
        List<VisaApplicationEntity> travellers = departure.getTravellers();
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        int n = 1;
        for (VisaApplicationEntity t : travellers) {
            rows.add(new String[]{
                    String.valueOf(n++), s(t.getSurname()), s(t.getGivenNames()), s(t.getGender()), s(t.getDateOfBirth()),
                    s(t.getNationality()), s(t.getPassportNumber()), s(t.getPassportDateOfExpiry()), t.getStatusEnum().name(),
                    s(t.getGovReferenceNo()), s(t.getEntryType()), s(t.getVisaValidTo())});
        }
        return rows;
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }

    private static String csv(String value) {
        if (value == null) return "";
        String v = value;

        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
