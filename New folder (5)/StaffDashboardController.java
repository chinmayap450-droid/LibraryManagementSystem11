
package com.library.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaffDashboardController {

    @GetMapping("/staff-dashboard")
    public String staffDashboard(HttpSession session) {

        if (!"STAFF".equals(session.getAttribute("role"))) {
            return "redirect:/login";
        }

        return "staff-dashboard";
    }
}
