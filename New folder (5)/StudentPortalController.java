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
public class StudentPortalController {

    private final JdbcTemplate jdbcTemplate;

    public StudentPortalController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================
    // STUDENT ACCESS CHECK
    // =========================

    private boolean isStudentLoggedIn(HttpSession session) {

        Object role = session.getAttribute("role");
        Object studentId = session.getAttribute("studentId");

        return role != null
                && role.toString().equals("STUDENT")
                && studentId != null;
    }


    // =========================
    // BROWSE BOOKS + SEARCH
    // =========================

    @GetMapping("/student/books")
    public String browseBooks(
            @RequestParam(value = "search", required = false) String search,
            HttpSession session,
            Model model) {

        if (!isStudentLoggedIn(session)) {
            return "redirect:/student-login";
        }

        List<Map<String, Object>> books;

        if (search == null || search.trim().isEmpty()) {

            books = jdbcTemplate.queryForList(
                    "SELECT book_id, title, author, category, quantity, available_quantity " +
                            "FROM books " +
                            "ORDER BY title"
            );

            model.addAttribute("search", "");

        } else {

            String keyword = "%" + search.trim() + "%";

            books = jdbcTemplate.queryForList(
                    "SELECT book_id, title, author, category, quantity, available_quantity " +
                            "FROM books " +
                            "WHERE CAST(book_id AS CHAR) LIKE ? " +
                            "OR title LIKE ? " +
                            "OR author LIKE ? " +
                            "OR category LIKE ? " +
                            "ORDER BY title",
                    keyword,
                    keyword,
                    keyword,
                    keyword
            );

            model.addAttribute("search", search);
        }

        model.addAttribute("books", books);
        model.addAttribute(
                "studentName",
                session.getAttribute("studentName")
        );
        model.addAttribute(
                "studentId",
                session.getAttribute("studentId")
        );

        return "student-books";
    }


    // =========================
    // MY PROFILE
    // =========================

    @GetMapping("/student/profile")
    public String myProfile(
            HttpSession session,
            Model model) {

        if (!isStudentLoggedIn(session)) {
            return "redirect:/student-login";
        }

        Integer studentId =
                (Integer) session.getAttribute("studentId");

        List<Map<String, Object>> students =
                jdbcTemplate.queryForList(
                        "SELECT student_id, name, email, phone, created_at " +
                                "FROM students " +
                                "WHERE student_id = ?",
                        studentId
                );

        if (students.isEmpty()) {

            session.invalidate();

            return "redirect:/student-login";
        }

        model.addAttribute(
                "student",
                students.get(0)
        );

        return "student-profile";
    }


    // =========================
    // MY BOOKS
    // =========================

    @GetMapping("/student/my-books")
    public String myBooks(
            HttpSession session,
            Model model) {

        if (!isStudentLoggedIn(session)) {
            return "redirect:/student-login";
        }

        Integer studentId =
                (Integer) session.getAttribute("studentId");

        List<Map<String, Object>> myBooks =
                jdbcTemplate.queryForList(

                        "SELECT ib.issue_id, " +
                                "ib.book_id, " +
                                "b.title, " +
                                "b.author, " +
                                "b.category, " +
                                "ib.issue_date, " +
                                "ib.due_date, " +
                                "ib.return_date, " +
                                "ib.fine, " +
                                "ib.status " +

                                "FROM issue_books ib " +

                                "JOIN books b " +
                                "ON ib.book_id = b.book_id " +

                                "WHERE ib.member_id = ? " +

                                "ORDER BY ib.issue_date DESC",

                        studentId
                );

        model.addAttribute(
                "myBooks",
                myBooks
        );

        model.addAttribute(
                "studentName",
                session.getAttribute("studentName")
        );

        return "student-my-books";
    }
}