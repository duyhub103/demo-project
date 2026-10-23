package com.pharma.wms.service.impl;

import com.pharma.wms.dto.request.LoginRequest;
import com.pharma.wms.dto.response.AuthResponse;
import com.pharma.wms.entity.User;
import com.pharma.wms.repository.UserRepository;
import com.pharma.wms.security.JwtTokenProvider;
import com.pharma.wms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        String token = jwtTokenProvider.generateToken(
            principal.getUsername(),
            principal.getRole(),
            principal.getWarehouseId()
        );

        return AuthResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .userId(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .role(user.getRole())
            .warehouseId(user.getWarehouse() != null ? user.getWarehouse().getId() : null)
            .warehouseName(user.getWarehouse() != null ? user.getWarehouse().getName() : "Toàn hệ thống")
            .build();
    }
}
