package com.pharma.wms.dto.response;

import com.pharma.wms.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String username;
    private String fullName;
    private Role role;
    private Long warehouseId;
    private String warehouseName;
}
