package com.example.demo.security;

import com.example.demo.model.StaffUser;
import com.example.demo.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffUserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) {
        StaffUser s = repo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return org.springframework.security.core.userdetails.User.withUsername(s.getUsername())
                .password(s.getPasswordHash())
                .roles(s.getRole().name())          // becomes ROLE_ANALYST / ROLE_ADMIN
                .disabled(!s.isEnabled())
                .build();
    }
}