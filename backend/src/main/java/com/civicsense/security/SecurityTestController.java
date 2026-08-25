package com.civicsense.security;

import com.civicsense.user.entity.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class SecurityTestController {

    @GetMapping("/protected")
    public String protectedEndpoint(
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return "Authenticated as: "
                + user.getName()
                + " | Mobile: "
                + user.getMobileNumber();
    }

    @GetMapping("/citizen")
    @PreAuthorize("hasRole('CITIZEN')")
    public String citizenEndpoint() {

        return "Citizen endpoint accessed successfully";
    }

    @GetMapping("/officer")
    @PreAuthorize("hasRole('OFFICER')")
    public String officerEndpoint() {

        return "Officer endpoint accessed successfully";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {

        return "Admin endpoint accessed successfully";
    }
}