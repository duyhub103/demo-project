package com.pharma.wms.repository;

import com.pharma.wms.entity.TransferOrder;
import com.pharma.wms.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferOrderRepository extends JpaRepository<TransferOrder, Long> {

    @Query("""
        SELECT t FROM TransferOrder t
        WHERE (:warehouseId IS NULL OR t.sourceWarehouse.id = :warehouseId OR t.destWarehouse.id = :warehouseId)
          AND (:status IS NULL OR t.status = :status)
        ORDER BY t.createdAt DESC
    """)
    Page<TransferOrder> findByWarehouseAndStatus(
        @Param("warehouseId") Long warehouseId,
        @Param("status") TransferStatus status,
        Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(t.id), 0) FROM TransferOrder t")
    Long getMaxId();
}
