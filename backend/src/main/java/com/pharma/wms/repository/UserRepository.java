package com.pharma.wms.repository;

import com.pharma.wms.entity.User;
import com.pharma.wms.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByWarehouseId(Long warehouseId);

    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.active = true AND (:warehouseId IS NULL OR u.warehouse.id = :warehouseId)")
    List<User> findActiveByWarehouse(Long warehouseId);
}
