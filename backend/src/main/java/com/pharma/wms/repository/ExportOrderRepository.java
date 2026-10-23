package com.pharma.wms.repository;

import com.pharma.wms.entity.ExportOrder;
import com.pharma.wms.enums.ExportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExportOrderRepository extends JpaRepository<ExportOrder, Long> {

    Optional<ExportOrder> findByOrderCode(String orderCode);

    @Query("""
        SELECT eo FROM ExportOrder eo
        WHERE (:warehouseId IS NULL OR eo.warehouse.id = :warehouseId)
          AND (:status IS NULL OR eo.status = :status)
        ORDER BY eo.createdAt DESC
    """)
    Page<ExportOrder> findByWarehouseAndStatus(
        @Param("warehouseId") Long warehouseId,
        @Param("status") ExportStatus status,
        Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(eo.id), 0) FROM ExportOrder eo")
    Long getMaxId();
}
