package com.pharma.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lô hàng - đơn vị cốt lõi cho cơ chế FEFO
 * Mỗi lô gắn với một thuốc và một kho, có số lượng tồn riêng.
 */
@Entity
@Table(name = "batches", indexes = {
    @Index(name = "idx_batch_number", columnList = "batchNumber"),
    @Index(name = "idx_batch_medicine_warehouse", columnList = "medicine_id,warehouse_id"),
    @Index(name = "idx_batch_expiry", columnList = "expiryDate")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String batchNumber; // Số lô

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private LocalDate manufactureDate; // Ngày sản xuất

    @Column(nullable = false)
    private LocalDate expiryDate; // Hạn sử dụng - KEY FIELD cho FEFO

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0; // Tồn hiện tại

    @Column(nullable = false)
    @Builder.Default
    private Integer initialQuantity = 0; // Số lượng ban đầu khi nhập

    @Column(precision = 15, scale = 2)
    private BigDecimal importPrice; // Giá nhập

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Optimistic Locking version - dùng cho các cập nhật không critical
     * Pessimistic Locking được dùng khi trừ tồn (SELECT FOR UPDATE)
     */
    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isNearExpiry(int warningDays) {
        return !isExpired() && LocalDate.now().plusDays(warningDays).isAfter(expiryDate);
    }
}
