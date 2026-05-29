package com.skillnet.mapper;

import com.skillnet.persistence.entity.core.Coupon;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.InfoproductorServiceOffering;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.CouponRepository;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.InfoproductorServiceOfferingRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.web.dto.request.PaymentRequestDTO;
import com.skillnet.web.dto.response.CouponSummaryDTO;
import com.skillnet.web.dto.response.PaymentResponseDTO;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CouponRepository couponRepository;
    private final InfoproductorServiceOfferingRepository serviceOfferingRepository;

    public PaymentMapper(
            UserMapper userMapper,
            CourseMapper courseMapper,
            UserRepository userRepository,
            CourseRepository courseRepository,
            CouponRepository couponRepository,
            InfoproductorServiceOfferingRepository serviceOfferingRepository) {
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.couponRepository = couponRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    public PaymentResponseDTO toResponseDTO(Payment payment) {
        if (payment == null) {
            return null;
        }
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setGatewayReference(resolveGatewayReference(payment));
        User user = payment.getUser();
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setUser(userMapper.toSummaryDTO(user));
        }
        Course course = payment.getCourse();
        if (course != null) {
            dto.setCourseId(course.getId());
            dto.setCourse(courseMapper.toSummaryDTO(course));
        }
        InfoproductorServiceOffering offering = payment.getServiceOffering();
        if (offering != null) {
            dto.setServiceOfferingId(offering.getId());
        }
        Coupon coupon = payment.getCoupon();
        if (coupon != null) {
            dto.setCouponId(coupon.getId());
            dto.setCoupon(toCouponSummaryDTO(coupon));
        }
        dto.setDocumentType(payment.getDocumentType());
        dto.setDocumentNumber(payment.getDocumentNumber());
        dto.setClientName(payment.getClientName());
        dto.setClientEmail(payment.getClientEmail());
        dto.setClientRut(payment.getClientRut());
        dto.setClientAddress(payment.getClientAddress());
        dto.setClientPhone(payment.getClientPhone());
        dto.setCompanyName(payment.getCompanyName());
        dto.setCompanyRut(payment.getCompanyRut());
        dto.setCompanyAddress(payment.getCompanyAddress());
        dto.setCompanyPhone(payment.getCompanyPhone());
        dto.setCompanyEmail(payment.getCompanyEmail());
        dto.setDocumentSent(payment.isDocumentSent());
        dto.setAccountingNotified(payment.isAccountingNotified());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        return dto;
    }

    public CouponSummaryDTO toCouponSummaryDTO(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        return new CouponSummaryDTO(
                coupon.getId(),
                coupon.getCode(),
                coupon.getPercentOff(),
                coupon.getAmountOff(),
                coupon.isActive());
    }

    public Payment toEntity(PaymentRequestDTO dto) {
        Payment payment = new Payment();
        applyToEntity(payment, dto, true);
        return payment;
    }

    public void applyToEntity(Payment payment, PaymentRequestDTO dto, boolean isCreate) {
        if (dto.getUserId() != null || isCreate) {
            payment.setUser(resolveUser(dto.getUserId()));
        }
        if (dto.getCourseId() != null) {
            payment.setCourse(resolveCourse(dto.getCourseId()));
        } else if (isCreate) {
            payment.setCourse(null);
        }
        if (dto.getServiceOfferingId() != null) {
            payment.setServiceOffering(resolveServiceOffering(dto.getServiceOfferingId()));
        } else if (isCreate) {
            payment.setServiceOffering(null);
        }
        if (dto.getCouponId() != null) {
            payment.setCoupon(resolveCoupon(dto.getCouponId()));
        } else if (isCreate) {
            payment.setCoupon(null);
        }
        if (dto.getAmount() != null) {
            payment.setAmount(dto.getAmount());
        }
        if (dto.getPaymentMethod() != null) {
            payment.setPaymentMethod(dto.getPaymentMethod());
        }
        if (dto.getStatus() != null) {
            payment.setStatus(dto.getStatus());
        }
        if (dto.getDocumentType() != null) {
            payment.setDocumentType(dto.getDocumentType());
        }
        payment.setDocumentNumber(dto.getDocumentNumber());
        payment.setClientName(dto.getClientName());
        payment.setClientEmail(dto.getClientEmail());
        payment.setClientRut(dto.getClientRut());
        payment.setClientAddress(dto.getClientAddress());
        payment.setClientPhone(dto.getClientPhone());
        if (dto.getCompanyName() != null) {
            payment.setCompanyName(dto.getCompanyName());
        }
        if (dto.getCompanyRut() != null) {
            payment.setCompanyRut(dto.getCompanyRut());
        }
        if (dto.getCompanyAddress() != null) {
            payment.setCompanyAddress(dto.getCompanyAddress());
        }
        if (dto.getCompanyPhone() != null) {
            payment.setCompanyPhone(dto.getCompanyPhone());
        }
        if (dto.getCompanyEmail() != null) {
            payment.setCompanyEmail(dto.getCompanyEmail());
        }
        payment.setDocumentSent(dto.isDocumentSent());
        payment.setAccountingNotified(dto.isAccountingNotified());
        if (dto.getCreatedAt() != null) {
            payment.setCreatedAt(dto.getCreatedAt());
        } else if (isCreate) {
            payment.setCreatedAt(Instant.now());
        }
        if (dto.getUpdatedAt() != null) {
            payment.setUpdatedAt(dto.getUpdatedAt());
        } else if (isCreate) {
            payment.setUpdatedAt(Instant.now());
        }
    }

    private String resolveGatewayReference(Payment payment) {
        if (payment.getStripeCheckoutId() != null) {
            return payment.getStripeCheckoutId();
        }
        if (payment.getPaypalOrderId() != null) {
            return payment.getPaypalOrderId();
        }
        if (payment.getMercadopagoPaymentId() != null) {
            return payment.getMercadopagoPaymentId();
        }
        if (payment.getDlocalPaymentId() != null) {
            return payment.getDlocalPaymentId();
        }
        if (payment.getIzipayTransactionId() != null) {
            return payment.getIzipayTransactionId();
        }
        return null;
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private Course resolveCourse(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findById(courseId).orElse(null);
    }

    private Coupon resolveCoupon(Long couponId) {
        if (couponId == null) {
            return null;
        }
        return couponRepository.findById(couponId).orElse(null);
    }

    private InfoproductorServiceOffering resolveServiceOffering(Long serviceOfferingId) {
        if (serviceOfferingId == null) {
            return null;
        }
        return serviceOfferingRepository.findById(serviceOfferingId).orElse(null);
    }
}
