package com.pharma.wms.repository;

import com.pharma.wms.entity.ImportOrder;
import com.pharma.wms.enums.ImportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportOrderRepository extends JpaRepository<ImportOrder, Long> {

    Optional<ImportOrder> findByOrderCode(String orderCode);

    @Query("""
        SELECT io FROM ImportOrder io
        WHERE (:warehouseId IS NULL OR io.warehouse.id = :warehouseId)
          AND (:status IS NULL OR io.status = :status)
        ORDER BY io.createdAt DESC
    """)
    Page<ImportOrder> findByWarehouseAndStatus(
        @Param("warehouseId") Long warehouseId,
        @Param("status") ImportStatus status,
        Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(io.id), 0) FROM ImportOrder io")
    Long getMaxId();
}
