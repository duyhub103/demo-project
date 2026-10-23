package com.pharma.wms.service.impl;

import com.pharma.wms.dto.request.CreateUserRequest;
import com.pharma.wms.entity.User;
import com.pharma.wms.entity.Warehouse;
import com.pharma.wms.exception.BusinessException;
import com.pharma.wms.exception.ResourceNotFoundException;
import com.pharma.wms.repository.UserRepository;
import com.pharma.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pharma.wms.dto.response.UserResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Tên đăng nhập đã tồn tại: " + request.getUsername());
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã được sử dụng: " + request.getEmail());
        }

        Warehouse warehouse = null;
        if (request.getWarehouseId() != null) {
            warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Kho hàng", request.getWarehouseId()));
        }

        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .email(request.getEmail())
            .role(request.getRole())
            .warehouse(warehouse)
            .active(true)
            .build();

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
        user.setActive(!user.isActive());
        return mapToResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
    }

    public UserResponse mapToResponse(User user) {
        UserResponse.WarehouseInfo warehouseInfo = null;
        if (user.getWarehouse() != null) {
            warehouseInfo = UserResponse.WarehouseInfo.builder()
                .id(user.getWarehouse().getId())
                .name(user.getWarehouse().getName())
                .code(user.getWarehouse().getCode())
                .build();
        }
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .role(user.getRole())
            .warehouse(warehouseInfo)
            .active(user.isActive())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
