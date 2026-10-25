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
import java.util.List;
import java.util.Map;

@Controller
public class IssueBookController {

    private final JdbcTemplate jdbcTemplate;

    public IssueBookController(JdbcTemplate jdbcTemplate) {
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
    // OPEN ISSUE BOOKS PAGE
    // =========================================================

    @GetMapping("/admin/issues")
    public String issueBooks(Model model, HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        loadPage(model, "");

        return "issue-books";
    }

    // =========================================================
    // ISSUE A BOOK
    // =========================================================

    @PostMapping("/admin/issues/issue")
    @Transactional
    public String issueBook(
            @RequestParam int studentId,
            @RequestParam int bookId,
            @RequestParam String issueDate,
            @RequestParam String dueDate,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        try {

            // -------------------------------------------------
            // 1. BASIC ID VALIDATION
            // -------------------------------------------------

            if (studentId <= 0) {
                model.addAttribute(
                        "error",
                        "Please enter a valid Student ID."
                );

                loadPage(model, "");
                return "issue-books";
            }

            if (bookId <= 0) {
                model.addAttribute(
                        "error",
                        "Please enter a valid Book ID."
                );

                loadPage(model, "");
                return "issue-books";
            }

            // -------------------------------------------------
            // 2. DATE VALIDATION
            // -------------------------------------------------

            if (issueDate == null || issueDate.trim().isEmpty()) {
                model.addAttribute(
                        "error",
                        "Please select the issue date."
                );

                loadPage(model, "");
                return "issue-books";
            }

            if (dueDate == null || dueDate.trim().isEmpty()) {
                model.addAttribute(
                        "error",
                        "Please select the due date."
                );

                loadPage(model, "");
                return "issue-books";
            }

            LocalDate issue;
            LocalDate due;

            try {
                issue = LocalDate.parse(issueDate);
                due = LocalDate.parse(dueDate);

            } catch (Exception e) {

                model.addAttribute(
                        "error",
                        "Invalid date format. Please select valid dates."
                );

                loadPage(model, "");
                return "issue-books";
            }

            if (due.isBefore(issue)) {

                model.addAttribute(
                        "error",
                        "Due date cannot be before the issue date."
                );

                loadPage(model, "");
                return "issue-books";
            }

            // -------------------------------------------------
            // 3. CHECK STUDENT
            // -------------------------------------------------

            Integer studentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM students WHERE student_id = ?",
                    Integer.class,
                    studentId
            );

            if (studentCount == null || studentCount == 0) {

                model.addAttribute(
                        "error",
                        "Student ID " + studentId + " was not found."
                );

                loadPage(model, "");
                return "issue-books";
            }

            // -------------------------------------------------
            // 4. CHECK BOOK
            // -------------------------------------------------

            List<Map<String, Object>> books = jdbcTemplate.queryForList(
                    """
                    SELECT book_id,
                           title,
                           author,
                           category,
                           quantity,
                           available_quantity
                    FROM books
                    WHERE book_id = ?
                    """,
                    bookId
            );

            if (books.isEmpty()) {

                model.addAttribute(
                        "error",
                        "Book ID " + bookId + " was not found."
                );

                loadPage(model, "");
                return "issue-books";
            }

            Map<String, Object> book = books.get(0);

            Number availableNumber =
                    (Number) book.get("available_quantity");

            int available = availableNumber == null
                    ? 0
                    : availableNumber.intValue();

            String bookTitle = String.valueOf(
                    book.get("title")
            );

            // -------------------------------------------------
            // 5. CHECK AVAILABILITY
            // -------------------------------------------------

            if (available <= 0) {

                model.addAttribute(
                        "error",
                        "No available copy of \"" +
                                bookTitle +
                                "\" is currently available."
                );

                loadPage(model, "");
                return "issue-books";
            }

            // -------------------------------------------------
            // 6. CHECK DUPLICATE ACTIVE ISSUE
            // -------------------------------------------------

            Integer alreadyIssued = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM issue_books
                    WHERE member_id = ?
                      AND book_id = ?
                      AND status = 'ISSUED'
                    """,
                    Integer.class,
                    studentId,
                    bookId
            );

            if (alreadyIssued != null && alreadyIssued > 0) {

                model.addAttribute(
                        "error",
                        "This student already has this book issued."
                );

                loadPage(model, "");
                return "issue-books";
            }

            // -------------------------------------------------
            // 7. INSERT ISSUE RECORD FIRST
            // -------------------------------------------------
            //
            // We insert the issue record first.
            // If this fails, the transaction stops and the
            // book availability is NOT changed.
            //
            // -------------------------------------------------

            int inserted = jdbcTemplate.update(
                    """
                    INSERT INTO issue_books
                    (
                        book_id,
                        member_id,
                        issue_date,
                        due_date,
                        return_date,
                        fine,
                        status
                    )
                    VALUES (?, ?, ?, ?, NULL, 0, 'ISSUED')
                    """,
                    bookId,
                    studentId,
                    Date.valueOf(issue),
                    Date.valueOf(due)
            );

            if (inserted != 1) {

                model.addAttribute(
                        "error",
                        "The issue record could not be created."
                );

                loadPage(model, "");
                return "issue-books";
            }

            // -------------------------------------------------
            // 8. DECREASE AVAILABLE QUANTITY
            // -------------------------------------------------

            int updated = jdbcTemplate.update(
                    """
                    UPDATE books
                    SET available_quantity = available_quantity - 1
                    WHERE book_id = ?
                      AND available_quantity > 0
                    """,
                    bookId
            );

            if (updated != 1) {

                // Because the method is @Transactional,
                // the previous INSERT will be rolled back.

                model.addAttribute(
                        "error",
                        "The book became unavailable while issuing. Please try again."
                );

                loadPage(model, "");
                return "issue-books";
            }

            // -------------------------------------------------
            // 9. SUCCESS
            // -------------------------------------------------

            model.addAttribute(
                    "success",
                    "Book \"" +
                            bookTitle +
                            "\" issued successfully to Student ID " +
                            studentId +
                            "."
            );

        } catch (DataAccessException ex) {

            // Database-related error
            System.err.println(
                    "DATABASE ERROR WHILE ISSUING BOOK:"
            );

            ex.printStackTrace();

            String message = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            model.addAttribute(
                    "error",
                    "Database error: " + message
            );

        } catch (Exception ex) {

            // Other error
            System.err.println(
                    "ERROR WHILE ISSUING BOOK:"
            );

            ex.printStackTrace();

            model.addAttribute(
                    "error",
                    "Could not issue the book: " +
                            ex.getMessage()
            );
        }

        loadPage(model, "");

        return "issue-books";
    }

    // =========================================================
    // SEARCH ISSUE RECORDS
    // =========================================================

    @GetMapping("/admin/issues/search")
    public String searchIssues(
            @RequestParam(defaultValue = "") String search,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        loadPage(model, search);

        model.addAttribute("search", search);

        return "issue-books";
    }

    // =========================================================
    // LOAD STUDENTS, BOOKS AND ISSUE RECORDS
    // =========================================================

    private void loadPage(Model model, String search) {

        // -----------------------------------------------------
        // LOAD STUDENTS
        // -----------------------------------------------------

        model.addAttribute(
                "students",
                jdbcTemplate.queryForList(
                        """
                        SELECT student_id,
                               name,
                               email,
                               phone
                        FROM students
                        ORDER BY student_id
                        """
                )
        );

        // -----------------------------------------------------
        // LOAD BOOKS
        // -----------------------------------------------------

        model.addAttribute(
                "books",
                jdbcTemplate.queryForList(
                        """
                        SELECT book_id,
                               title,
                               author,
                               available_quantity
                        FROM books
                        ORDER BY book_id
                        """
                )
        );

        // -----------------------------------------------------
        // SEARCH VALUE
        // -----------------------------------------------------

        String value =
                "%" +
                        (search == null ? "" : search.trim()) +
                        "%";

        // -----------------------------------------------------
        // LOAD ISSUE RECORDS
        // -----------------------------------------------------

        List<Map<String, Object>> issues =
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
                        WHERE CAST(i.issue_id AS CHAR) LIKE ?
                           OR CAST(i.member_id AS CHAR) LIKE ?
                           OR CAST(i.book_id AS CHAR) LIKE ?
                           OR s.name LIKE ?
                           OR b.title LIKE ?
                        ORDER BY i.issue_id DESC
                        """,
                        value,
                        value,
                        value,
                        value,
                        value
                );

        model.addAttribute(
                "issues",
                issues
        );
    }
}