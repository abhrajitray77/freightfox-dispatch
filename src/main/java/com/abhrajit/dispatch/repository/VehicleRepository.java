package com.abhrajit.dispatch.repository;

import com.abhrajit.dispatch.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
}
