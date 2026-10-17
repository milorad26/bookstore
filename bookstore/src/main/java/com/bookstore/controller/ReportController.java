package com.bookstore.controller;

import com.bookstore.dto.InventoryReportDTO;
import com.bookstore.dto.SalesReportDTO;
import com.bookstore.service.ReportExcelExportService;
import com.bookstore.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Sales and inventory reports")
public class ReportController {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    );

    private final ReportService reportService;
    private final ReportExcelExportService reportExcelExportService;

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Value("${inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Operation(summary = "Get sales report")
    @GetMapping("/sales")
    public ResponseEntity<SalesReportDTO> getSalesReport(
            @Parameter(description = "Accepted formats: yyyy-MM-dd, dd.MM.yyyy, or dd/MM/yyyy")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "Accepted formats: yyyy-MM-dd, dd.MM.yyyy, or dd/MM/yyyy")
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(buildSalesReport(startDate, endDate));
    }

    @Operation(summary = "Export sales report as an Excel workbook")
    @GetMapping(value = "/sales/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportSalesReport(
            @Parameter(description = "Accepted formats: yyyy-MM-dd, dd.MM.yyyy, or dd/MM/yyyy")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "Accepted formats: yyyy-MM-dd, dd.MM.yyyy, or dd/MM/yyyy")
            @RequestParam(required = false) String endDate) {
        byte[] workbook = reportExcelExportService.exportSalesReport(buildSalesReport(startDate, endDate));
        return excelDownload(workbook, "sales-report.xlsx");
    }

    private LocalDate parseDate(String parameterName, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported date format.
            }
        }

        throw new IllegalArgumentException("Invalid " + parameterName
                + ": use yyyy-MM-dd, dd.MM.yyyy, or dd/MM/yyyy.");
    }

    @Operation(summary = "Get inventory report")
    @GetMapping("/inventory")
    public ResponseEntity<InventoryReportDTO> getInventoryReport() {
        return ResponseEntity.ok(reportService.getInventoryReport(lowStockThreshold));
    }

    @Operation(summary = "Export inventory report as an Excel workbook")
    @GetMapping(value = "/inventory/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportInventoryReport() {
        InventoryReportDTO report = reportService.getInventoryReport(lowStockThreshold);
        return excelDownload(reportExcelExportService.exportInventoryReport(report), "inventory-report.xlsx");
    }

    private SalesReportDTO buildSalesReport(String startDate, String endDate) {
        LocalDate parsedEndDate = parseDate("endDate", endDate);
        LocalDate parsedStartDate = parseDate("startDate", startDate);

        if (parsedStartDate == null) {
            throw new IllegalArgumentException("Please select a start date.");
        }
        if (parsedEndDate == null) {
            throw new IllegalArgumentException("Please select an end date.");
        }
        if (parsedEndDate.isBefore(parsedStartDate)) {
            throw new IllegalArgumentException("End date must be on or after the start date.");
        }

        return reportService.getSalesReport(parsedStartDate, parsedEndDate);
    }

    private ResponseEntity<byte[]> excelDownload(byte[] workbook, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(workbook);
    }
}
