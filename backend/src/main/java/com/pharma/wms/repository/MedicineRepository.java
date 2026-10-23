package com.pharma.wms.repository;

import com.pharma.wms.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.active = true
          AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(m.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(m.activeIngredient) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Medicine> searchMedicines(@Param("keyword") String keyword, Pageable pageable);
}
