package com.pharma.wms.repository;

import com.pharma.wms.entity.InventoryCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {

    @Query("""
        SELECT ic FROM InventoryCheck ic
        WHERE (:warehouseId IS NULL OR ic.warehouse.id = :warehouseId)
        ORDER BY ic.createdAt DESC
    """)
    Page<InventoryCheck> findByWarehouse(@Param("warehouseId") Long warehouseId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(ic.id), 0) FROM InventoryCheck ic")
    Long getMaxId();
}
