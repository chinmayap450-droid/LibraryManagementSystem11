
package com.library.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class ReportsController {

    private final JdbcTemplate jdbcTemplate;

    public ReportsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/admin/reports")
    public String reports(
            @RequestParam(defaultValue = "") String search,
            Model model,
            HttpSession session) {

        // Only Admin can access Reports.
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/login";
        }

        Integer totalTitles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM books", Integer.class);

        Integer totalCopies = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM books",
                Integer.class);

        Integer availableCopies = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(available_quantity), 0) FROM books",
                Integer.class);

        Integer issuedBooks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM issue_books WHERE status = 'ISSUED'",
                Integer.class);

        Integer returnedBooks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM issue_books WHERE status = 'RETURNED'",
                Integer.class);

        Integer totalStudents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students", Integer.class);

        Integer totalStaff = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM members", Integer.class);

        Double totalFines = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(fine), 0)
                FROM issue_books
                WHERE status = 'RETURNED'
                """,
                Double.class);

        model.addAttribute("totalTitles", totalTitles);
        model.addAttribute("totalCopies", totalCopies);
        model.addAttribute("availableCopies", availableCopies);
        model.addAttribute("issuedBooks", issuedBooks);
        model.addAttribute("returnedBooks", returnedBooks);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalStaff", totalStaff);
        model.addAttribute(
                "totalFines",
                totalFines == null ? 0.0 : totalFines
        );

        String value = "%" + search.trim() + "%";

        List<Map<String, Object>> issueHistory =
                jdbcTemplate.queryForList(
                        """
                        SELECT i.issue_id,
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
                           OR i.status LIKE ?
                        ORDER BY i.issue_id DESC
                        """,
                        value, value, value, value, value, value
                );

        model.addAttribute("issueHistory", issueHistory);
        model.addAttribute("search", search);

        return "reports";
    }
}