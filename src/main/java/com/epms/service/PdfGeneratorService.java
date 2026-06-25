package com.epms.service;

import com.epms.entity.Employee;
import com.epms.entity.Payroll;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PdfGeneratorService — generates professional salary slip PDFs using Apache PDFBox.
 *
 * <p>Creates an A4 PDF with:
 * <ul>
 *   <li>Branded header with company name and "SALARY SLIP" title</li>
 *   <li>Employee info section (name, designation, department, email, joining date)</li>
 *   <li>Pay period and generated date</li>
 *   <li>Earnings & deductions table with net salary summary</li>
 * </ul>
 */
@Slf4j
@Service
public class PdfGeneratorService {

    private static final PDType1Font FONT_BOLD    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final float MARGIN       = 50f;
    private static final float PAGE_WIDTH   = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT  = PDRectangle.A4.getHeight();
    private static final float CONTENT_W    = PAGE_WIDTH - 2 * MARGIN;

    private static final Color COLOR_PRIMARY = new Color(30, 41, 99);   // dark navy
    private static final Color COLOR_ACCENT  = new Color(99, 102, 241); // indigo
    private static final Color COLOR_LIGHT   = new Color(241, 245, 249);
    private static final Color COLOR_TEXT    = new Color(30, 30, 30);
    private static final Color COLOR_MUTED   = new Color(100, 116, 139);
    private static final Color COLOR_SUCCESS = new Color(22, 163, 74);

    private static final java.text.DecimalFormat CURRENCY_FMT;
    static {
        CURRENCY_FMT = new java.text.DecimalFormat("#,##,##0.00");
    }

    /**
     * Generates a salary slip PDF and saves it to {@code outputPath}.
     *
     * @param payroll    the payroll record (must have employee and department loaded)
     * @param outputPath the absolute path to write the PDF to
     */
    public void generate(Payroll payroll, Path outputPath) throws IOException {
        log.info("[PdfGeneratorService] Generating salary slip PDF → {}", outputPath);

        Employee emp = payroll.getEmployee();
        String empName = emp.getFirstName() + " " + emp.getLastName();
        String dept    = emp.getDepartment() != null ? emp.getDepartment().getName() : "—";
        String period  = monthName(payroll.getMonth()) + " " + payroll.getYear();
        String genDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float y = PAGE_HEIGHT - MARGIN;

                /**
                 *  Header banner 
                 */
                drawRect(cs, MARGIN, y - 60, CONTENT_W, 60, COLOR_PRIMARY, true);
                y -= 60;

                /**
                 *  Company name
                 */
                cs.beginText();
                cs.setFont(FONT_BOLD, 20);
                cs.setNonStrokingColor(Color.WHITE);
                cs.newLineAtOffset(MARGIN + 16, y + 38);
                cs.showText("EMSPro");
                cs.endText();

                /**
                 *  Subtitle
                 */
                cs.beginText();
                cs.setFont(FONT_REGULAR, 9);
                cs.setNonStrokingColor(new Color(200, 210, 255));
                cs.newLineAtOffset(MARGIN + 16, y + 22);
                cs.showText("Employee Management & Payroll System");
                cs.endText();

                /**
                 *  "SALARY SLIP" on right
                 */
                String slipLabel = "SALARY SLIP";
                float slipLabelW = FONT_BOLD.getStringWidth(slipLabel) / 1000f * 14;
                cs.beginText();
                cs.setFont(FONT_BOLD, 14);
                cs.setNonStrokingColor(Color.WHITE);
                cs.newLineAtOffset(MARGIN + CONTENT_W - slipLabelW - 16, y + 38);
                cs.showText(slipLabel);
                cs.endText();

                /**
                 *  Period on right
                 */
                String periodLabel = "Period: " + period;
                float periodLabelW = FONT_REGULAR.getStringWidth(periodLabel) / 1000f * 9;
                cs.beginText();
                cs.setFont(FONT_REGULAR, 9);
                cs.setNonStrokingColor(new Color(200, 210, 255));
                cs.newLineAtOffset(MARGIN + CONTENT_W - periodLabelW - 16, y + 22);
                cs.showText(periodLabel);
                cs.endText();

                y -= 20;

                y = drawSectionHeader(cs, "Employee Information", y - 10);

                float col1X = MARGIN;
                float col2X = MARGIN + CONTENT_W / 2f;
                float lineH = 22f;

                y -= 12;
                drawLabelValue(cs, "Employee Name",  empName,              col1X, y);
                drawLabelValue(cs, "Employee ID",    "EMP-" + emp.getId(), col2X, y);
                y -= lineH;
                drawLabelValue(cs, "Designation",    emp.getDesignation(),  col1X, y);
                drawLabelValue(cs, "Department",     dept,                  col2X, y);
                y -= lineH;
                drawLabelValue(cs, "Email",          emp.getEmail(),        col1X, y);
                drawLabelValue(cs, "Phone",          emp.getPhone(),        col2X, y);
                y -= lineH;
                String joinDate = emp.getJoiningDate() != null
                        ? emp.getJoiningDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
                drawLabelValue(cs, "Joining Date",   joinDate,              col1X, y);
                drawLabelValue(cs, "Generated On",   genDate,               col2X, y);
                y -= lineH;
                drawLabelValue(cs, "Pay Period",     period,                col1X, y);
                drawLabelValue(cs, "Payroll ID",     "#" + payroll.getId(), col2X, y);

                y -= 24;

                y = drawSectionHeader(cs, "Earnings", y - 10);
                BigDecimal gross = payroll.getGrossSalary();
                BigDecimal basic = gross.multiply(BigDecimal.valueOf(0.50)).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal hra = gross.multiply(BigDecimal.valueOf(0.30)).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal allowances = gross.multiply(BigDecimal.valueOf(0.20)).setScale(2, java.math.RoundingMode.HALF_UP);

                y = drawTableRow(cs, "Gross Monthly Salary (Base)", fmt(payroll.getBaseSalary()), y - 4, COLOR_LIGHT, false);
                y = drawTableRow(cs, "Gross Salary (Pro-rated)",     fmt(gross),                   y,     Color.WHITE, false);
                y = drawTableRow(cs, "Basic Salary (50%)",           fmt(basic),                   y,     COLOR_LIGHT, false);
                y = drawTableRow(cs, "House Rent Allowance (30%)",   fmt(hra),                     y,     Color.WHITE, false);
                y = drawTableRow(cs, "Special Allowance (20%)",      fmt(allowances),              y,     COLOR_LIGHT, false);
                y = drawTableRow(cs, "Bonus",                        fmt(payroll.getBonus()),      y,     Color.WHITE, false);

                y = drawSectionHeader(cs, "Deductions", y - 10);
                y = drawTableRow(cs, "Provident Fund (PF)",          fmt(payroll.getPf()),                 y - 4, COLOR_LIGHT, false);
                y = drawTableRow(cs, "Professional Tax",             fmt(payroll.getTax()),                y,     Color.WHITE, false);
                y = drawTableRow(cs, "Attendance Deductions",        fmt(payroll.getAttendanceDeduction()),y,     COLOR_LIGHT, false);
                y = drawTableRow(cs, "Other Deductions",             fmt(payroll.getDeductions()),         y,     Color.WHITE, false);

                y -= 16;

                /**
                 *  Net Salary summary box
                 */
                float boxH = 44f;
                drawRect(cs, MARGIN, y - boxH, CONTENT_W, boxH, COLOR_PRIMARY, true);

                cs.beginText();
                cs.setFont(FONT_BOLD, 11);
                cs.setNonStrokingColor(Color.WHITE);
                cs.newLineAtOffset(MARGIN + 16, y - 16);
                cs.showText("NET SALARY");
                cs.endText();

                String netStr = fmt(payroll.getNetSalary());
                float netStrW = FONT_BOLD.getStringWidth(netStr) / 1000f * 16;
                cs.beginText();
                cs.setFont(FONT_BOLD, 16);
                cs.setNonStrokingColor(new Color(167, 243, 208));
                cs.newLineAtOffset(MARGIN + CONTENT_W - netStrW - 16, y - 20);
                cs.showText(netStr);
                cs.endText();

                y -= boxH + 20;

                String status = payroll.getPayrollStatus().name();
                Color statusColor = "PAID".equals(status) ? COLOR_SUCCESS : COLOR_ACCENT;
                drawRect(cs, MARGIN, y - 20, 80, 20, statusColor, true);
                cs.beginText();
                cs.setFont(FONT_BOLD, 9);
                cs.setNonStrokingColor(Color.WHITE);
                cs.newLineAtOffset(MARGIN + 8, y - 13);
                cs.showText(status);
                cs.endText();

                y -= 36;

                // ── Footer ───────────────────────────────────────────────────
                drawLine(cs, MARGIN, y, MARGIN + CONTENT_W, y, COLOR_MUTED);
                y -= 14;
                drawText(cs, FONT_REGULAR, 8, COLOR_MUTED,
                        "This is a system-generated salary slip and does not require a signature.",
                        MARGIN, y);
                y -= 12;
                drawText(cs, FONT_REGULAR, 8, COLOR_MUTED,
                        "Generated by EMSPro Payroll Management System  •  " + genDate,
                        MARGIN, y);
            }

            // Ensure parent dirs exist
            outputPath.getParent().toFile().mkdirs();
            doc.save(outputPath.toFile());
            log.info("[PdfGeneratorService] PDF saved → {}", outputPath);
        }
    }


    private float drawSectionHeader(PDPageContentStream cs, String title, float y) throws IOException {
        drawRect(cs, MARGIN, y - 20, CONTENT_W, 20, COLOR_ACCENT, true);
        cs.beginText();
        cs.setFont(FONT_BOLD, 10);
        cs.setNonStrokingColor(Color.WHITE);
        cs.newLineAtOffset(MARGIN + 8, y - 14);
        cs.showText(title.toUpperCase());
        cs.endText();
        return y - 20;
    }

    private float drawTableRow(PDPageContentStream cs, String label, String value,
                               float y, Color bg, boolean bold) throws IOException {
        float rowH = 20f;
        drawRect(cs, MARGIN, y - rowH, CONTENT_W, rowH, bg, true);

        // label
        cs.beginText();
        cs.setFont(bold ? FONT_BOLD : FONT_REGULAR, 10);
        cs.setNonStrokingColor(COLOR_TEXT);
        cs.newLineAtOffset(MARGIN + 8, y - 14);
        cs.showText(label);
        cs.endText();

        // value right-aligned
        float valW = FONT_BOLD.getStringWidth(value) / 1000f * 10;
        cs.beginText();
        cs.setFont(FONT_BOLD, 10);
        cs.setNonStrokingColor(COLOR_TEXT);
        cs.newLineAtOffset(MARGIN + CONTENT_W - valW - 8, y - 14);
        cs.showText(value);
        cs.endText();

        // bottom border
        drawLine(cs, MARGIN, y - rowH, MARGIN + CONTENT_W, y - rowH, new Color(220, 220, 230));
        return y - rowH;
    }

    private void drawLabelValue(PDPageContentStream cs, String label, String value,
                                float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(FONT_REGULAR, 8);
        cs.setNonStrokingColor(COLOR_MUTED);
        cs.newLineAtOffset(x, y);
        cs.showText(label + ":");
        cs.endText();

        cs.beginText();
        cs.setFont(FONT_BOLD, 9);
        cs.setNonStrokingColor(COLOR_TEXT);
        cs.newLineAtOffset(x, y - 11);
        cs.showText(value != null ? value : "—");
        cs.endText();
    }

    private void drawRect(PDPageContentStream cs, float x, float y, float w, float h,
                          Color color, boolean fill) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        if (fill) cs.fill(); else cs.stroke();
    }

    private void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2,
                          Color color) throws IOException {
        cs.setStrokingColor(color);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float size,
                          Color color, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    /**
     * Utilities
     * @param val
     * @return
     */

    private String fmt(BigDecimal val) {
        if (val == null) return "Rs.0.00";
        return "Rs." + CURRENCY_FMT.format(val);
    }

    private String monthName(int month) {
        String[] names = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return (month >= 1 && month <= 12) ? names[month - 1] : String.valueOf(month);
    }
}
