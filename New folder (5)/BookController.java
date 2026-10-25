package com.library.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class BookController {

    private final JdbcTemplate jdbcTemplate;

    public BookController(JdbcTemplate jdbcTemplate) {
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
    // OPEN MANAGE BOOKS PAGE
    // =========================================================

    @GetMapping("/admin/books")
    public String manageBooks(
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        loadBooks(model, "");

        return "manage-books";
    }

    // =========================================================
    // ADD BOOK
    // =========================================================

    @PostMapping("/admin/books/add")
    public String addBook(
            @RequestParam int bookId,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String category,
            @RequestParam int quantity,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        if (bookId <= 0) {

            model.addAttribute(
                    "error",
                    "Book ID must be greater than 0."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        if (title == null || title.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Book title is required."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        if (author == null || author.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Author name is required."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        if (category == null || category.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Category is required."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        if (quantity <= 0) {

            model.addAttribute(
                    "error",
                    "Quantity must be greater than 0."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        // Check duplicate Book ID
        Integer existingBook = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM books
                WHERE book_id = ?
                """,
                Integer.class,
                bookId
        );

        if (existingBook != null && existingBook > 0) {

            model.addAttribute(
                    "error",
                    "Book ID " + bookId + " already exists."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        // Insert book
        String sql = """
                INSERT INTO books
                (
                    book_id,
                    title,
                    author,
                    category,
                    quantity,
                    available_quantity
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                bookId,
                title.trim(),
                author.trim(),
                category.trim(),
                quantity,
                quantity
        );

        model.addAttribute(
                "success",
                "Book added successfully."
        );

        loadBooks(model, "");

        return "manage-books";
    }

    // =========================================================
    // UPDATE BOOK
    // =========================================================

    @PostMapping("/admin/books/update")
    public String updateBook(
            @RequestParam int bookId,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String category,
            @RequestParam int quantity,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        if (bookId <= 0) {

            model.addAttribute(
                    "error",
                    "Please enter or select a valid Book ID."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        if (title == null || title.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Book title is required."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        if (author == null || author.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Author name is required."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        if (category == null || category.trim().isEmpty()) {

            model.addAttribute(
                    "error",
                    "Category is required."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        if (quantity <= 0) {

            model.addAttribute(
                    "error",
                    "Quantity must be greater than 0."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        // =====================================================
        // COUNT ONLY CURRENTLY ISSUED COPIES
        // =====================================================

        Integer issuedCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM issue_books
                WHERE book_id = ?
                  AND status = 'ISSUED'
                """,
                Integer.class,
                bookId
        );

        if (issuedCount == null) {
            issuedCount = 0;
        }

        // Quantity cannot be smaller than active issued copies
        if (quantity < issuedCount) {

            model.addAttribute(
                    "error",
                    "Quantity cannot be less than currently issued copies ("
                            + issuedCount + ")."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        // Calculate available copies
        int availableQuantity = quantity - issuedCount;

        // Update book
        String sql = """
                UPDATE books
                SET title = ?,
                    author = ?,
                    category = ?,
                    quantity = ?,
                    available_quantity = ?
                WHERE book_id = ?
                """;

        int updatedRows = jdbcTemplate.update(
                sql,
                title.trim(),
                author.trim(),
                category.trim(),
                quantity,
                availableQuantity,
                bookId
        );

        if (updatedRows == 0) {

            model.addAttribute(
                    "error",
                    "Book ID " + bookId + " was not found."
            );

        } else {

            model.addAttribute(
                    "success",
                    "Book updated successfully."
            );
        }

        loadBooks(model, "");

        return "manage-books";
    }

    // =========================================================
    // DELETE BOOK
    // =========================================================

    @PostMapping("/admin/books/delete")
    public String deleteBook(
            @RequestParam int bookId,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        if (bookId <= 0) {

            model.addAttribute(
                    "error",
                    "Please enter or select a valid Book ID."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        // Only active ISSUED records prevent deletion
        Integer issuedCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM issue_books
                WHERE book_id = ?
                  AND status = 'ISSUED'
                """,
                Integer.class,
                bookId
        );

        if (issuedCount != null && issuedCount > 0) {

            model.addAttribute(
                    "error",
                    "Cannot delete this book because "
                            + issuedCount
                            + " copy/copies are currently issued."
            );

            loadBooks(model, "");

            return "manage-books";
        }

        // Delete book
        int deletedRows = jdbcTemplate.update(
                "DELETE FROM books WHERE book_id = ?",
                bookId
        );

        if (deletedRows == 0) {

            model.addAttribute(
                    "error",
                    "Book ID " + bookId + " was not found."
            );

        } else {

            model.addAttribute(
                    "success",
                    "Book deleted successfully."
            );
        }

        loadBooks(model, "");

        return "manage-books";
    }

    // =========================================================
    // SEARCH BOOKS
    // =========================================================

    @GetMapping("/admin/books/search")
    public String searchBooks(
            @RequestParam(defaultValue = "") String search,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        loadBooks(model, search);

        model.addAttribute(
                "search",
                search
        );

        return "manage-books";
    }

    // =========================================================
    // LOAD BOOKS
    // =========================================================

    private void loadBooks(
            Model model,
            String search) {

        String sql;

        List<Map<String, Object>> books;

        // =====================================================
        // IMPORTANT:
        // ISSUED QUANTITY IS CALCULATED DIRECTLY FROM
        // issue_books WHERE status = 'ISSUED'
        // =====================================================

        if (search == null || search.trim().isEmpty()) {

            sql = """
                    SELECT
                        b.book_id,
                        b.title,
                        b.author,
                        b.category,
                        b.quantity,
                        b.available_quantity,

                        (
                            SELECT COUNT(*)
                            FROM issue_books i
                            WHERE i.book_id = b.book_id
                              AND i.status = 'ISSUED'
                        ) AS issued_quantity

                    FROM books b

                    ORDER BY b.book_id
                    """;

            books = jdbcTemplate.queryForList(sql);

        } else {

            String value = "%" + search.trim() + "%";

            sql = """
                    SELECT
                        b.book_id,
                        b.title,
                        b.author,
                        b.category,
                        b.quantity,
                        b.available_quantity,

                        (
                            SELECT COUNT(*)
                            FROM issue_books i
                            WHERE i.book_id = b.book_id
                              AND i.status = 'ISSUED'
                        ) AS issued_quantity

                    FROM books b

                    WHERE CAST(b.book_id AS CHAR) LIKE ?
                       OR b.title LIKE ?
                       OR b.author LIKE ?
                       OR b.category LIKE ?

                    ORDER BY b.book_id
                    """;

            books = jdbcTemplate.queryForList(
                    sql,
                    value,
                    value,
                    value,
                    value
            );
        }

        // =====================================================
        // SEND BOOK DATA TO HTML
        // =====================================================

        model.addAttribute(
                "books",
                books
        );
    }
}