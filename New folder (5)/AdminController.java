
package com.library.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {

    private final JdbcTemplate jdbcTemplate;

    public AdminController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/admin-login")
    public String adminLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        try {
            String sql = """
                    SELECT COUNT(*)
                    FROM admin
                    WHERE username = ? AND password = ?
                    """;

            Integer count = jdbcTemplate.queryForObject(
                    sql,
                    Integer.class,
                    username.trim(),
                    password
            );

            if (count != null && count > 0) {
                session.setAttribute("role", "ADMIN");
                session.setAttribute("adminUsername", username.trim());

                session.removeAttribute("staffIdentifier");

                return "redirect:/admin-dashboard";
            }

            model.addAttribute(
                    "error",
                    "Invalid username or password"
            );

            return "admin-login";

        } catch (Exception e) {
            model.addAttribute(
                    "error",
                    "Unable to log in. Please check your database connection."
            );

            return "admin-login";
        }
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/login";
        }

        return "admin-dashboard";
    }
}