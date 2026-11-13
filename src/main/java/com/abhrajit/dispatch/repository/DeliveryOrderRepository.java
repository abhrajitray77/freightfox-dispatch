package com.abhrajit.dispatch.repository;

import com.abhrajit.dispatch.domain.DeliveryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, String> {
}
