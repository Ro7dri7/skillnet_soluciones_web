package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.payments.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUser_Id(Long userId);

    List<Payment> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByStatus(String status);

    List<Payment> findByUser_IdAndStatus(Long userId, String status);

    List<Payment> findByCourse_Id(Long courseId);

    Optional<Payment> findByMercadopagoPaymentId(String mercadopagoPaymentId);

    Optional<Payment> findByDlocalPaymentId(String dlocalPaymentId);
}
