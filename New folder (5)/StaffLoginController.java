
package com.library.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StaffLoginController {

    private final JdbcTemplate jdbcTemplate;

    public StaffLoginController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/staff-login")
    public String staffLogin(
            @RequestParam("identifier") String identifier,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        try {
            String sql = """
                    SELECT COUNT(*)
                    FROM members
                    WHERE (LOWER(email) = LOWER(?) OR phone = ?)
                    AND password = ?
                    """;

            Integer count = jdbcTemplate.queryForObject(
                    sql,
                    Integer.class,
                    identifier.trim(),
                    identifier.trim(),
                    password
            );

            if (count != null && count > 0) {
                session.setAttribute("role", "STAFF");
                session.setAttribute("staffIdentifier", identifier.trim());

                return "redirect:/staff-dashboard";
            }

            model.addAttribute(
                    "error",
                    "Invalid email/phone or password. Please try again."
            );

            return "staff-login";

        } catch (Exception e) {
            model.addAttribute(
                    "error",
                    "Unable to log in. Please check your database connection."
            );

            return "staff-login";
        }
    }
}