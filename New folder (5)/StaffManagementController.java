
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
public class StaffManagementController {

    private final JdbcTemplate jdbcTemplate;

    public StaffManagementController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================================
    // ADMIN ACCESS CHECK
    // =========================================================

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("role"));
    }

    // =========================================================
    // OPEN MANAGE STAFF PAGE
    // =========================================================

    @GetMapping("/admin/staff")
    public String manageStaff(Model model, HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        loadStaff(model, "");
        return "manage-staff";
    }

    // =========================================================
    // ADD STAFF
    // =========================================================

    @PostMapping("/admin/staff/add")
    public String addStaff(
            @RequestParam int memberId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (memberId <= 0) {
            model.addAttribute("error", "Staff ID must be greater than 0.");
            loadStaff(model, "");
            return "manage-staff";
        }

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Staff name is required.");
            loadStaff(model, "");
            return "manage-staff";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Email is required.");
            loadStaff(model, "");
            return "manage-staff";
        }

        if (phone == null || phone.trim().isEmpty()) {
            model.addAttribute("error", "Phone number is required.");
            loadStaff(model, "");
            return "manage-staff";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Password is required.");
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check Staff ID
        Integer idExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM members
                WHERE member_id = ?
                """,
                Integer.class,
                memberId
        );

        if (idExists != null && idExists > 0) {
            model.addAttribute(
                    "error",
                    "Staff ID " + memberId + " already exists."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check email across students
        Integer studentEmailExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE email = ?
                """,
                Integer.class,
                email.trim()
        );

        if (studentEmailExists != null && studentEmailExists > 0) {
            model.addAttribute(
                    "error",
                    "This email is already used by a student."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check email across staff
        Integer staffEmailExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM members
                WHERE email = ?
                """,
                Integer.class,
                email.trim()
        );

        if (staffEmailExists != null && staffEmailExists > 0) {
            model.addAttribute(
                    "error",
                    "This email is already used by another staff member."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check phone across students
        Integer studentPhoneExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE phone = ?
                """,
                Integer.class,
                phone.trim()
        );

        if (studentPhoneExists != null && studentPhoneExists > 0) {
            model.addAttribute(
                    "error",
                    "This phone number is already used by a student."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check phone across staff
        Integer staffPhoneExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM members
                WHERE phone = ?
                """,
                Integer.class,
                phone.trim()
        );

        if (staffPhoneExists != null && staffPhoneExists > 0) {
            model.addAttribute(
                    "error",
                    "This phone number is already used by another staff member."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Insert staff
        String sql = """
                INSERT INTO members
                    (member_id, name, email, phone, password)
                VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                memberId,
                name.trim(),
                email.trim(),
                phone.trim(),
                password.trim()
        );

        model.addAttribute(
                "success",
                "Staff member added successfully."
        );

        loadStaff(model, "");
        return "manage-staff";
    }

    // =========================================================
    // UPDATE STAFF
    // =========================================================

    @PostMapping("/admin/staff/update")
    public String updateStaff(
            @RequestParam int memberId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (memberId <= 0) {
            model.addAttribute(
                    "error",
                    "Please enter or select a valid Staff ID."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Staff name is required.");
            loadStaff(model, "");
            return "manage-staff";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Email is required.");
            loadStaff(model, "");
            return "manage-staff";
        }

        if (phone == null || phone.trim().isEmpty()) {
            model.addAttribute("error", "Phone number is required.");
            loadStaff(model, "");
            return "manage-staff";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Password is required.");
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check email in another staff account
        Integer staffEmailExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM members
                WHERE email = ?
                  AND member_id <> ?
                """,
                Integer.class,
                email.trim(),
                memberId
        );

        if (staffEmailExists != null && staffEmailExists > 0) {
            model.addAttribute(
                    "error",
                    "This email is already used by another staff member."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check email across students
        Integer studentEmailExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE email = ?
                """,
                Integer.class,
                email.trim()
        );

        if (studentEmailExists != null && studentEmailExists > 0) {
            model.addAttribute(
                    "error",
                    "This email is already used by a student."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check phone in another staff account
        Integer staffPhoneExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM members
                WHERE phone = ?
                  AND member_id <> ?
                """,
                Integer.class,
                phone.trim(),
                memberId
        );

        if (staffPhoneExists != null && staffPhoneExists > 0) {
            model.addAttribute(
                    "error",
                    "This phone number is already used by another staff member."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Check phone across students
        Integer studentPhoneExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM students
                WHERE phone = ?
                """,
                Integer.class,
                phone.trim()
        );

        if (studentPhoneExists != null && studentPhoneExists > 0) {
            model.addAttribute(
                    "error",
                    "This phone number is already used by a student."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        // Update staff
        String sql = """
                UPDATE members
                SET name = ?,
                    email = ?,
                    phone = ?,
                    password = ?
                WHERE member_id = ?
                """;

        int updatedRows = jdbcTemplate.update(
                sql,
                name.trim(),
                email.trim(),
                phone.trim(),
                password.trim(),
                memberId
        );

        if (updatedRows == 0) {
            model.addAttribute(
                    "error",
                    "Staff ID " + memberId + " was not found."
            );
        } else {
            model.addAttribute(
                    "success",
                    "Staff member updated successfully."
            );
        }

        loadStaff(model, "");
        return "manage-staff";
    }

    // =========================================================
    // DELETE STAFF
    // =========================================================

    @PostMapping("/admin/staff/delete")
    public String deleteStaff(
            @RequestParam int memberId,
            Model model,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (memberId <= 0) {
            model.addAttribute(
                    "error",
                    "Please enter or select a valid Staff ID."
            );
            loadStaff(model, "");
            return "manage-staff";
        }

        int deletedRows = jdbcTemplate.update(
                "DELETE FROM members WHERE member_id = ?",
                memberId
        );

        if (deletedRows == 0) {
            model.addAttribute(
                    "error",
                    "Staff ID " + memberId + " was not found."
            );
        } else {
            model.addAttribute(
                    "success",
                    "Staff member deleted successfully."
            );
        }

        loadStaff(model, "");
        return "manage-staff";
    }

    // =========================================================
    // SEARCH STAFF
    // =========================================================

    @GetMapping("/admin/staff/search")
    public String searchStaff(
            @RequestParam(defaultValue = "") String search,
            Model model,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        loadStaff(model, search);
        model.addAttribute("search", search);

        return "manage-staff";
    }

    // =========================================================
    // LOAD STAFF
    // =========================================================

    private void loadStaff(Model model, String search) {

        String sql;
        List<Map<String, Object>> staff;

        // View all staff
        if (search == null || search.trim().isEmpty()) {

            sql = """
                    SELECT member_id,
                           name,
                           email,
                           phone,
                           password
                    FROM members
                    ORDER BY member_id
                    """;

            staff = jdbcTemplate.queryForList(sql);

        } else {

            // Search staff
            String value = "%" + search.trim() + "%";

            sql = """
                    SELECT member_id,
                           name,
                           email,
                           phone,
                           password
                    FROM members
                    WHERE CAST(member_id AS CHAR) LIKE ?
                       OR name LIKE ?
                       OR email LIKE ?
                       OR phone LIKE ?
                    ORDER BY member_id
                    """;

            staff = jdbcTemplate.queryForList(
                    sql,
                    value,
                    value,
                    value,
                    value
            );
        }

        model.addAttribute("staff", staff);
    }
}