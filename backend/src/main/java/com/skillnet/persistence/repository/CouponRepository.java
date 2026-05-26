package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Coupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findByApplicableCourse_IdOrderByIdDesc(Long courseId);
}
