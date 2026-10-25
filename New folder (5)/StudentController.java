
package com.library.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class StudentController {

    private static final Logger logger =
            LoggerFactory.getLogger(StudentController.class);

    private final JdbcTemplate jdbcTemplate;

    public StudentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Student registration page
    @GetMapping("/student-register")
    public String studentRegistrationPage() {
        return "student-register";
    }

    // Register a student
    @PostMapping("/student-register")
    public String registerStudent(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        name = name.trim();
        email = email.trim();
        phone = phone.trim();

        if (name.isEmpty() || email.isEmpty()
                || phone.isEmpty() || password.isEmpty()) {
            model.addAttribute("error", "Please fill in all fields.");
            return "student-register";
        }

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            model.addAttribute("error", "Please enter a valid email address.");
            return "student-register";
        }

        if (!phone.matches("\\d{10}")) {
            model.addAttribute(
                    "error", "Phone number must contain exactly 10 digits.");
            return "student-register";
        }

        if (password.length() < 6) {
            model.addAttribute(
                    "error", "Password must contain at least 6 characters.");
            return "student-register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "student-register";
        }

        try {
            String duplicateSql = """
                    SELECT COUNT(*)
                    FROM students
                    WHERE LOWER(email) = LOWER(?)
                       OR phone = ?
                    """;

            Integer count = jdbcTemplate.queryForObject(
                    duplicateSql, Integer.class, email, phone);

            if (count != null && count > 0) {
                model.addAttribute(
                        "error",
                        "This email or phone number is already registered.");
                return "student-register";
            }

            String insertSql = """
                    INSERT INTO students (name, phone, email, password)
                    VALUES (?, ?, ?, ?)
                    """;

            jdbcTemplate.update(
                    insertSql, name, phone, email, password);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Registration successful! You can now log in.");

            return "redirect:/login";

        } catch (Exception e) {
            logger.error("Student registration failed", e);
            model.addAttribute(
                    "error",
                    "Registration failed. Check the application console for details.");
            return "student-register";
        }
    }

    // Student login
    @PostMapping("/student-login")
    public String studentLogin(
            @RequestParam String login,
            @RequestParam String password,
            HttpSession session,
            HttpServletRequest request,
            Model model) {

        try {
            String sql = """
                    SELECT student_id, name, email, phone
                    FROM students
                    WHERE (LOWER(email) = LOWER(?) OR phone = ?)
                      AND password = ?
                    """;

            List<StudentData> students = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new StudentData(
                            rs.getInt("student_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("phone")
                    ),
                    login.trim(),
                    login.trim(),
                    password
            );

            if (students.isEmpty()) {
                model.addAttribute(
                        "error", "Invalid email/phone or password.");
                return "student-login";
            }

            StudentData student = students.get(0);

            // Prevent the previous user's session ID from being reused.
            request.changeSessionId();

            session.removeAttribute("adminUsername");
            session.removeAttribute("staffIdentifier");
            session.removeAttribute("studentId");
            session.removeAttribute("studentName");
            session.removeAttribute("studentEmail");

            session.setAttribute("role", "STUDENT");
            session.setAttribute("studentId", student.getStudentId());
            session.setAttribute("studentName", student.getName());
            session.setAttribute("studentEmail", student.getEmail());

            return "redirect:/student-dashboard";

        } catch (Exception e) {
            // Log the actual cause in IntelliJ instead of hiding it.
            logger.error("Student login failed", e);

            model.addAttribute(
                    "error",
                    "Student login encountered a server error. Check the IntelliJ console.");
            return "student-login";
        }
    }

    // Student dashboard
    @GetMapping("/student-dashboard")
    public String studentDashboard(
            HttpSession session,
            Model model) {

        if (!"STUDENT".equals(session.getAttribute("role"))
                || session.getAttribute("studentId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("studentName",
                session.getAttribute("studentName"));
        model.addAttribute("studentId",
                session.getAttribute("studentId"));
        model.addAttribute("studentEmail",
                session.getAttribute("studentEmail"));

        return "student-dashboard";
    }

    // Student data
    public static class StudentData {

        private final int studentId;
        private final String name;
        private final String email;
        private final String phone;

        public StudentData(
                int studentId,
                String name,
                String email,
                String phone) {
            this.studentId = studentId;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        public int getStudentId() {
            return studentId;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }
    }
}
