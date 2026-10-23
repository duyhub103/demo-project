package com.pharma.wms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "transfer_order_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_order_id", nullable = false)
    private TransferOrder transferOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_batch_id", nullable = false)
    private Batch sourceBatch; // Lô ở kho nguồn

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dest_batch_id")
    private Batch destBatch; // Lô được tạo ở kho đích sau khi DELIVERED

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer quantity;

    // Copy batch info để tạo lô ở kho đích
    private String batchNumber;
    private LocalDate expiryDate;
    private LocalDate manufactureDate;
}
