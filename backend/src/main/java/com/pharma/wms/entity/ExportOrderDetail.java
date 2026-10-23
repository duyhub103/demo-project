package com.pharma.wms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "export_order_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExportOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "export_order_id", nullable = false)
    private ExportOrder exportOrder;

    /**
     * Lô hàng được hệ thống tự chọn theo FEFO (hạn dùng gần nhất trước)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer quantity; // Số lượng lấy từ lô này
}
