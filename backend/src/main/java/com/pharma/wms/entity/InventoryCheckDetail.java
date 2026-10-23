package com.pharma.wms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_check_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryCheckDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_check_id", nullable = false)
    private InventoryCheck inventoryCheck;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer systemQuantity; // Tồn theo hệ thống

    private Integer actualQuantity; // Tồn thực đếm

    // Chênh lệch = actualQuantity - systemQuantity (tự tính)
    public Integer getDifference() {
        if (actualQuantity == null) return null;
        return actualQuantity - systemQuantity;
    }

    @Column(length = 300)
    private String note;
}
