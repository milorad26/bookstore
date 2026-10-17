package com.bookstore.service;

import com.bookstore.dto.BookDTO;
import com.bookstore.dto.InventoryReportDTO;
import com.bookstore.dto.SalesReportDTO;
import com.bookstore.dto.TopSellingBookDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ReportExcelExportService {

    public byte[] exportSalesReport(SalesReportDTO report) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales report");
            CellStyle headerStyle = createHeaderStyle(workbook);

            addRow(sheet, 0, headerStyle, "Start date", "End date", "Paid orders", "Units sold", "Revenue");
            addRow(sheet, 1, null, report.getStartDate(), report.getEndDate(), report.getOrderCount(),
                    report.getUnitsSold(), report.getRevenue());

            Sheet booksSheet = workbook.createSheet("Top selling books");
            addRow(booksSheet, 0, headerStyle, "Book ID", "Title", "Author", "Quantity sold", "Sales amount");
            int rowIndex = 1;
            for (TopSellingBookDTO book : report.getTopSellingBooks()) {
                addRow(booksSheet, rowIndex++, null, book.getBookId(), book.getTitle(), book.getAuthor(),
                        book.getQuantitySold(), book.getSalesAmount());
            }

            autoSizeColumns(sheet, 5);
            autoSizeColumns(booksSheet, 5);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the sales report workbook.", exception);
        }
    }

    public byte[] exportInventoryReport(InventoryReportDTO report) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory report");
            CellStyle headerStyle = createHeaderStyle(workbook);

            addRow(sheet, 0, headerStyle, "Titles in catalog", "Units in stock", "Out of stock",
                    "Low-stock books", "Low-stock threshold", "Inventory value");
            addRow(sheet, 1, null, report.getTitleCount(), report.getUnitsInStock(), report.getOutOfStockCount(),
                    report.getLowStockCount(), report.getLowStockThreshold(), report.getInventoryValue());

            Sheet booksSheet = workbook.createSheet("Low-stock books");
            addRow(booksSheet, 0, headerStyle, "Book ID", "Title", "Author", "ISBN", "Stock", "Unit price");
            int rowIndex = 1;
            for (BookDTO book : report.getLowStockBooks()) {
                addRow(booksSheet, rowIndex++, null, book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                        book.getStockQuantity(), book.getPrice());
            }

            autoSizeColumns(sheet, 6);
            autoSizeColumns(booksSheet, 6);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the inventory report workbook.", exception);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void addRow(Sheet sheet, int rowIndex, CellStyle style, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            Cell cell = row.createCell(columnIndex);
            if (style != null) {
                cell.setCellStyle(style);
            }
            Object value = values[columnIndex];
            if (value == null) {
                continue;
            }
            if (value instanceof Number number) {
                cell.setCellValue(new BigDecimal(number.toString()).doubleValue());
            } else if (value instanceof LocalDate date) {
                cell.setCellValue(date.toString());
            } else {
                cell.setCellValue(value.toString());
            }
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
        }
    }
}
