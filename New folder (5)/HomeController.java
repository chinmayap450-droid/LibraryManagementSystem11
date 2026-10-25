
package com.library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Home page
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Main login selection page
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Admin login page
    @GetMapping("/admin-login")
    public String adminLogin() {
        return "admin-login";
    }

    // Staff login page
    @GetMapping("/staff-login")
    public String staffLogin() {
        return "staff-login";
    }

    // Student login page
    @GetMapping("/student-login")
    public String studentLogin() {
        return "student-login";
    }

}