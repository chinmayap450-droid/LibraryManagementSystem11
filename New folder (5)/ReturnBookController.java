package com.library.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Controller
public class ReturnBookController {

    private final JdbcTemplate jdbcTemplate;

    private static final double FINE_PER_DAY = 5.0;

    public ReturnBookController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================================
    // ACCESS CHECK: ADMIN OR STAFF
    // =========================================================

    private boolean hasAccess(HttpSession session) {

        Object role = session.getAttribute("role");

        return "ADMIN".equals(role) || "STAFF".equals(role);
    }

    // =========================================================
    // OPEN RETURN BOOKS PAGE
    // =========================================================

    @GetMapping("/admin/returns")
    public String returnBooks(
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        loadPage(model, "");

        return "return-books";
    }

    // =========================================================
    // RETURN A BOOK
    // =========================================================

    @PostMapping("/admin/returns/return")
    @Transactional
    public String returnBook(
            @RequestParam int studentId,
            @RequestParam int bookId,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        try {

            // -------------------------------------------------
            // 1. VALIDATE IDs
            // -------------------------------------------------

            if (studentId <= 0 || bookId <= 0) {

                model.addAttribute(
                        "error",
                        "Enter a valid Student ID and Book ID."
                );

                loadPage(model, "");

                return "return-books";
            }

            // -------------------------------------------------
            // 2. CHECK STUDENT
            // -------------------------------------------------

            Integer studentCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM students
                    WHERE student_id = ?
                    """,
                    Integer.class,
                    studentId
            );

            if (studentCount == null || studentCount == 0) {

                model.addAttribute(
                        "error",
                        "Student ID " + studentId + " was not found."
                );

                loadPage(model, "");

                return "return-books";
            }

            // -------------------------------------------------
            // 3. CHECK BOOK
            // -------------------------------------------------

            List<Map<String, Object>> bookRecords =
                    jdbcTemplate.queryForList(
                            """
                            SELECT
                                book_id,
                                title,
                                quantity,
                                available_quantity
                            FROM books
                            WHERE book_id = ?
                            FOR UPDATE
                            """,
                            bookId
                    );

            if (bookRecords.isEmpty()) {

                model.addAttribute(
                        "error",
                        "Book ID " + bookId + " was not found."
                );

                loadPage(model, "");

                return "return-books";
            }

            Map<String, Object> book =
                    bookRecords.get(0);

            String bookTitle =
                    String.valueOf(book.get("title"));

            int totalQuantity =
                    ((Number) book.get("quantity")).intValue();

            int currentAvailable =
                    ((Number) book.get("available_quantity")).intValue();

            // -------------------------------------------------
            // 4. FIND ACTIVE ISSUE
            // -------------------------------------------------

            List<Map<String, Object>> records =
                    jdbcTemplate.queryForList(
                            """
                            SELECT
                                issue_id,
                                due_date
                            FROM issue_books
                            WHERE member_id = ?
                              AND book_id = ?
                              AND status = 'ISSUED'
                            ORDER BY issue_id ASC
                            LIMIT 1
                            FOR UPDATE
                            """,
                            studentId,
                            bookId
                    );

            if (records.isEmpty()) {

                model.addAttribute(
                        "error",
                        "No active issue found for Student ID "
                                + studentId
                                + " and Book ID "
                                + bookId
                                + "."
                );

                loadPage(model, "");

                return "return-books";
            }

            // -------------------------------------------------
            // 5. GET ISSUE INFORMATION
            // -------------------------------------------------

            Map<String, Object> record =
                    records.get(0);

            int issueId =
                    ((Number) record.get("issue_id")).intValue();

            Object dueDateObject =
                    record.get("due_date");

            LocalDate dueDate;

            if (dueDateObject instanceof java.sql.Date) {

                dueDate =
                        ((java.sql.Date) dueDateObject)
                                .toLocalDate();

            } else if (dueDateObject instanceof LocalDate) {

                dueDate =
                        (LocalDate) dueDateObject;

            } else {

                dueDate =
                        LocalDate.parse(
                                dueDateObject.toString()
                        );
            }

            // -------------------------------------------------
            // 6. CALCULATE FINE
            // -------------------------------------------------

            LocalDate today =
                    LocalDate.now();

            long overdueDays =
                    Math.max(
                            0,
                            ChronoUnit.DAYS.between(
                                    dueDate,
                                    today
                            )
                    );

            double fine =
                    overdueDays * FINE_PER_DAY;

            // -------------------------------------------------
            // 7. MARK ISSUE AS RETURNED
            // -------------------------------------------------

            int issueUpdated =
                    jdbcTemplate.update(
                            """
                            UPDATE issue_books
                            SET
                                return_date = ?,
                                fine = ?,
                                status = 'RETURNED'
                            WHERE issue_id = ?
                              AND status = 'ISSUED'
                            """,
                            Date.valueOf(today),
                            fine,
                            issueId
                    );

            if (issueUpdated != 1) {

                throw new IllegalStateException(
                        "The issue record could not be marked as returned."
                );
            }

            // -------------------------------------------------
            // 8. INCREASE AVAILABLE QUANTITY
            // -------------------------------------------------
            //
            // Increase by exactly ONE copy.
            //
            // Never allow available copies to become greater
            // than the total number of copies.
            //
            // -------------------------------------------------

            if (currentAvailable >= totalQuantity) {

                throw new IllegalStateException(
                        "The book already has all copies available. "
                                + "The database availability is inconsistent."
                );
            }

            int bookUpdated =
                    jdbcTemplate.update(
                            """
                            UPDATE books
                            SET available_quantity =
                                available_quantity + 1
                            WHERE book_id = ?
                              AND available_quantity < quantity
                            """,
                            bookId
                    );

            if (bookUpdated != 1) {

                throw new IllegalStateException(
                        "Book availability could not be updated."
                );
            }

            // -------------------------------------------------
            // 9. SUCCESS MESSAGE
            // -------------------------------------------------

            model.addAttribute(
                    "success",
                    "Book \""
                            + bookTitle
                            + "\" returned successfully. "
                            + "Student ID: "
                            + studentId
                            + ". Overdue days: "
                            + overdueDays
                            + ". Fine: ₹"
                            + String.format("%.2f", fine)
            );

        } catch (DataAccessException ex) {

            // -------------------------------------------------
            // DATABASE ERROR
            // -------------------------------------------------

            ex.printStackTrace();

            String message;

            if (ex.getMostSpecificCause() != null) {

                message =
                        ex.getMostSpecificCause()
                                .getMessage();

            } else {

                message = ex.getMessage();
            }

            model.addAttribute(
                    "error",
                    "Database error while returning the book: "
                            + message
            );

        } catch (Exception ex) {

            // -------------------------------------------------
            // OTHER ERROR
            // -------------------------------------------------

            ex.printStackTrace();

            model.addAttribute(
                    "error",
                    "Unable to return the book: "
                            + ex.getMessage()
            );
        }

        loadPage(model, "");

        return "return-books";
    }

    // =========================================================
    // SEARCH RETURN HISTORY
    // =========================================================

    @GetMapping("/admin/returns/search")
    public String searchReturns(
            @RequestParam(defaultValue = "") String search,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        loadPage(model, search);

        model.addAttribute(
                "search",
                search
        );

        return "return-books";
    }

    // =========================================================
    // LOAD ACTIVE ISSUES AND RETURN HISTORY
    // =========================================================

    private void loadPage(
            Model model,
            String search) {

        // =====================================================
        // ACTIVE ISSUES
        // =====================================================

        model.addAttribute(
                "activeIssues",
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            i.issue_id,
                            i.member_id AS student_id,
                            s.name AS student_name,
                            i.book_id,
                            b.title AS book_title,
                            i.issue_date,
                            i.due_date,
                            i.fine,
                            i.status
                        FROM issue_books i
                        LEFT JOIN students s
                            ON s.student_id = i.member_id
                        LEFT JOIN books b
                            ON b.book_id = i.book_id
                        WHERE i.status = 'ISSUED'
                        ORDER BY i.due_date ASC
                        """
                )
        );

        // =====================================================
        // SEARCH VALUE
        // =====================================================

        String value =
                "%"
                        + (search == null
                        ? ""
                        : search.trim())
                        + "%";

        // =====================================================
        // RETURN HISTORY
        // =====================================================

        model.addAttribute(
                "returnHistory",
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            i.issue_id,
                            i.member_id AS student_id,
                            s.name AS student_name,
                            i.book_id,
                            b.title AS book_title,
                            i.issue_date,
                            i.due_date,
                            i.return_date,
                            i.fine,
                            i.status
                        FROM issue_books i
                        LEFT JOIN students s
                            ON s.student_id = i.member_id
                        LEFT JOIN books b
                            ON b.book_id = i.book_id
                        WHERE i.status = 'RETURNED'
                          AND (
                              CAST(i.issue_id AS CHAR) LIKE ?
                              OR CAST(i.member_id AS CHAR) LIKE ?
                              OR CAST(i.book_id AS CHAR) LIKE ?
                              OR s.name LIKE ?
                              OR b.title LIKE ?
                          )
                        ORDER BY i.issue_id DESC
                        """,
                        value,
                        value,
                        value,
                        value,
                        value
                )
        );
    }
}