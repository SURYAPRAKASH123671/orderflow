package com.orderflow.order.repository;

import com.orderflow.order.domain.CustomerOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findById(Long id);
}
