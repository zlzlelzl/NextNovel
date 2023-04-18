package com.example.payment.point.repository;

import com.example.payment.point.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByMemberId(Long id);
    /*
    select * from point where id = "" and point = "";
     */
}