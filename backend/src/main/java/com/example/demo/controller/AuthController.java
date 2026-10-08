package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.StaffResponse;
import com.example.demo.service.StaffUserService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final StaffUserService staffService;
    private final SecurityContextRepository contextRepo = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public StaffResponse login(@Valid @RequestBody LoginRequest req,
                               HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication auth = authManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(req.username(), req.password()));

            if (request.getSession(false) != null) request.changeSessionId();   // session-fixation protection
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
            contextRepo.saveContext(ctx, request, response);

            staffService.recordLogin(auth.getName());
            return staffService.findByUsername(auth.getName());
        } catch (AuthenticationException e) {
            // same message for wrong user, wrong password, or disabled account
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    /** Frontend calls this on page load to find out if a session already exists. */
    @GetMapping("/me")
    public StaffResponse me(Authentication auth) {
        return staffService.findByUsername(auth.getName());
    }
    // POST /api/auth/logout is handled by the logout() config in SecurityConfig
}