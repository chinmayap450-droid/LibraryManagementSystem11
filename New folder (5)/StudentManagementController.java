
package com.library.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class StudentManagementController {

    private final JdbcTemplate jdbcTemplate;

    public StudentManagementController(JdbcTemplate jdbcTemplate) {
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
    // OPEN MANAGE STUDENTS PAGE
    // =========================================================

    @GetMapping("/admin/students")
    public String manageStudents(Model model, HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        loadStudents(model, "");
        return "manage-students";
    }

    // =========================================================
    // ADD STUDENT
    // =========================================================

    @PostMapping("/admin/students/add")
    public String addStudent(
            @RequestParam int studentId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        if (studentId <= 0) {
            model.addAttribute("error", "Student ID must be greater than 0.");
            loadStudents(model, "");
            return "manage-students";
        }

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Student name is required.");
            loadStudents(model, "");
            return "manage-students";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Email is required.");
            loadStudents(model, "");
            return "manage-students";
        }

        if (phone == null || phone.trim().isEmpty()) {
            model.addAttribute("error", "Phone number is required.");
            loadStudents(model, "");
            return "manage-students";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Password is required.");
            loadStudents(model, "");
            return "manage-students";
        }

        // Check Student ID
        Integer idExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE student_id = ?
                """,
                Integer.class,
                studentId
        );

        if (idExists != null && idExists > 0) {
            model.addAttribute(
                    "error",
                    "Student ID " + studentId + " already exists."
            );
            loadStudents(model, "");
            return "manage-students";
        }

        // Check email
        Integer emailExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE email = ?
                """,
                Integer.class,
                email.trim()
        );

        if (emailExists != null && emailExists > 0) {
            model.addAttribute(
                    "error",
                    "This email is already registered."
            );
            loadStudents(model, "");
            return "manage-students";
        }

        // Check phone
        Integer phoneExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE phone = ?
                """,
                Integer.class,
                phone.trim()
        );

        if (phoneExists != null && phoneExists > 0) {
            model.addAttribute(
                    "error",
                    "This phone number is already registered."
            );
            loadStudents(model, "");
            return "manage-students";
        }

        // Add student
        String sql = """
                INSERT INTO students
                    (student_id, name, phone, email, password)
                VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                studentId,
                name.trim(),
                phone.trim(),
                email.trim(),
                password.trim()
        );

        model.addAttribute("success", "Student added successfully.");

        loadStudents(model, "");
        return "manage-students";
    }

    // =========================================================
    // UPDATE STUDENT
    // =========================================================

    @PostMapping("/admin/students/update")
    public String updateStudent(
            @RequestParam int studentId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        if (studentId <= 0) {
            model.addAttribute(
                    "error",
                    "Please enter or select a valid Student ID."
            );
            loadStudents(model, "");
            return "manage-students";
        }

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Student name is required.");
            loadStudents(model, "");
            return "manage-students";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Email is required.");
            loadStudents(model, "");
            return "manage-students";
        }

        if (phone == null || phone.trim().isEmpty()) {
            model.addAttribute("error", "Phone number is required.");
            loadStudents(model, "");
            return "manage-students";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Password is required.");
            loadStudents(model, "");
            return "manage-students";
        }

        // Check email for another student
        Integer emailExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE email = ?
                  AND student_id <> ?
                """,
                Integer.class,
                email.trim(),
                studentId
        );

        if (emailExists != null && emailExists > 0) {
            model.addAttribute(
                    "error",
                    "This email is already used by another student."
            );
            loadStudents(model, "");
            return "manage-students";
        }

        // Check phone for another student
        Integer phoneExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE phone = ?
                  AND student_id <> ?
                """,
                Integer.class,
                phone.trim(),
                studentId
        );

        if (phoneExists != null && phoneExists > 0) {
            model.addAttribute(
                    "error",
                    "This phone number is already used by another student."
            );
            loadStudents(model, "");
            return "manage-students";
        }

        // Update student
        String sql = """
                UPDATE students
                SET name = ?,
                    phone = ?,
                    email = ?,
                    password = ?
                WHERE student_id = ?
                """;

        int updatedRows = jdbcTemplate.update(
                sql,
                name.trim(),
                phone.trim(),
                email.trim(),
                password.trim(),
                studentId
        );

        if (updatedRows == 0) {
            model.addAttribute(
                    "error",
                    "Student ID " + studentId + " was not found."
            );
        } else {
            model.addAttribute("success", "Student updated successfully.");
        }

        loadStudents(model, "");
        return "manage-students";
    }

    // =========================================================
    // DELETE STUDENT
    // =========================================================

    @PostMapping("/admin/students/delete")
    public String deleteStudent(
            @RequestParam int studentId,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        if (studentId <= 0) {
            model.addAttribute(
                    "error",
                    "Please enter or select a valid Student ID."
            );
            loadStudents(model, "");
            return "manage-students";
        }

        // Check currently issued books
        Integer issuedCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM issue_books
                WHERE member_id = ?
                  AND status = 'ISSUED'
                """,
                Integer.class,
                studentId
        );

        if (issuedCount != null && issuedCount > 0) {
            model.addAttribute(
                    "error",
                    "Cannot delete this student because books are currently issued."
            );
            loadStudents(model, "");
            return "manage-students";
        }

        // Delete student
        int deletedRows = jdbcTemplate.update(
                "DELETE FROM students WHERE student_id = ?",
                studentId
        );

        if (deletedRows == 0) {
            model.addAttribute(
                    "error",
                    "Student ID " + studentId + " was not found."
            );
        } else {
            model.addAttribute("success", "Student deleted successfully.");
        }

        loadStudents(model, "");
        return "manage-students";
    }

    // =========================================================
    // SEARCH STUDENTS
    // =========================================================

    @GetMapping("/admin/students/search")
    public String searchStudents(
            @RequestParam(defaultValue = "") String search,
            Model model,
            HttpSession session) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        loadStudents(model, search);
        model.addAttribute("search", search);

        return "manage-students";
    }

    // =========================================================
    // LOAD STUDENTS
    // =========================================================

    private void loadStudents(Model model, String search) {

        String sql;
        List<Map<String, Object>> students;

        if (search == null || search.trim().isEmpty()) {

            sql = """
                    SELECT student_id,
                           name,
                           phone,
                           email,
                           password,
                           created_at
                    FROM students
                    ORDER BY student_id
                    """;

            students = jdbcTemplate.queryForList(sql);

        } else {

            String value = "%" + search.trim() + "%";

            sql = """
                    SELECT student_id,
                           name,
                           phone,
                           email,
                           password,
                           created_at
                    FROM students
                    WHERE CAST(student_id AS CHAR) LIKE ?
                       OR name LIKE ?
                       OR phone LIKE ?
                       OR email LIKE ?
                    ORDER BY student_id
                    """;

            students = jdbcTemplate.queryForList(
                    sql,
                    value,
                    value,
                    value,
                    value
            );
        }

        model.addAttribute("students", students);
    }
}