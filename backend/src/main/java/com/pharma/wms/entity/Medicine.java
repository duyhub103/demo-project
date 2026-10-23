package com.pharma.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicines", indexes = {
    @Index(name = "idx_medicine_code", columnList = "code"),
    @Index(name = "idx_medicine_active_ingredient", columnList = "activeIngredient")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, nullable = false, length = 30)
    private String code;

    @Column(length = 200)
    private String activeIngredient; // Hoạt chất

    @Column(length = 100)
    private String dosageForm; // Dạng bào chế (viên, gói, ống, ...)

    @Column(length = 50)
    private String unit; // Đơn vị tính (viên, hộp, lọ, ...)

    @Column(length = 100)
    private String concentration; // Hàm lượng

    @Column(length = 100)
    private String manufacturer; // Nhà sản xuất

    @Column(length = 50)
    private String registrationNumber; // Số đăng ký

    @Column(precision = 15, scale = 2)
    private BigDecimal unitPrice; // Giá đơn vị

    @Column(nullable = false)
    private Integer minStockLevel; // Mức tồn tối thiểu

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 500)
    private String description;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
