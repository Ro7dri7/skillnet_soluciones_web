package com.skillnet.service.payments;

import com.skillnet.api.dto.payments.CheckoutQuoteLineDTO;
import com.skillnet.api.dto.payments.CheckoutQuoteResponseDTO;
import com.skillnet.domain.CourseStatus;
import com.skillnet.persistence.entity.core.Coupon;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.repository.CouponRepository;
import com.skillnet.persistence.repository.CourseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CheckoutQuoteService {

    private final CourseRepository courseRepository;
    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public CheckoutQuoteResponseDTO quote(List<Long> courseIds, String couponCode) {
        if (courseIds == null || courseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar al menos un curso");
        }

        Map<Long, Course> coursesById = new LinkedHashMap<>();
        for (Long courseId : courseIds) {
            Course course = courseRepository
                    .findById(courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado: " + courseId));
            if (!CourseStatus.PUBLISHED.getDbValue().equals(course.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso no está publicado: " + course.getTitle());
            }
            coursesById.putIfAbsent(courseId, course);
        }

        List<Course> courses = new ArrayList<>(coursesById.values());
        List<CheckoutQuoteLineDTO> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Course course : courses) {
            BigDecimal price = normalizeMoney(course.getPrice());
            subtotal = subtotal.add(price);
            lines.add(CheckoutQuoteLineDTO.builder()
                    .courseId(course.getId())
                    .title(course.getTitle())
                    .baseAmount(price)
                    .totalAmount(price)
                    .build());
        }

        Optional<Coupon> couponOpt = resolveCoupon(couponCode, courses);
        BigDecimal discount = BigDecimal.ZERO;
        String message = null;
        boolean couponValid = false;
        Integer percentOff = null;
        BigDecimal amountOff = null;
        String appliedCode = null;
        String couponLabel = null;

        if (couponCode != null && !couponCode.isBlank()) {
            if (couponOpt.isEmpty()) {
                message = "Cupón inválido o no aplicable a estos cursos";
            } else {
                Coupon coupon = couponOpt.get();
                couponValid = true;
                appliedCode = coupon.getCode();
                percentOff = coupon.getPercentOff() > 0 ? coupon.getPercentOff() : null;
                amountOff = coupon.getAmountOff() != null && coupon.getAmountOff().signum() > 0
                        ? coupon.getAmountOff()
                        : null;

                List<CheckoutQuoteLineDTO> discountedLines = new ArrayList<>();
                for (CheckoutQuoteLineDTO line : lines) {
                    Course course = coursesById.get(line.getCourseId());
                    if (!isCouponApplicableToCourse(coupon, course)) {
                        discountedLines.add(line);
                        continue;
                    }
                    BigDecimal lineDiscount = calculateLineDiscount(line.getBaseAmount(), coupon);
                    BigDecimal lineTotal = line.getBaseAmount().subtract(lineDiscount).max(BigDecimal.ZERO);
                    discount = discount.add(lineDiscount);
                    discountedLines.add(CheckoutQuoteLineDTO.builder()
                            .courseId(line.getCourseId())
                            .title(line.getTitle())
                            .baseAmount(line.getBaseAmount())
                            .totalAmount(lineTotal)
                            .build());
                }
                lines = discountedLines;
                couponLabel = buildCouponLabel(coupon);
                message = "Cupón \"" + coupon.getCode() + "\" aplicado correctamente";
            }
        }

        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        return CheckoutQuoteResponseDTO.builder()
                .subtotal(subtotal)
                .discount(discount)
                .total(total)
                .couponCode(appliedCode)
                .couponPercentOff(percentOff)
                .couponAmountOff(amountOff)
                .couponLabel(couponLabel)
                .couponValid(couponValid)
                .message(message)
                .lines(lines)
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<Coupon> resolveCoupon(String couponCode, List<Course> courses) {
        if (couponCode == null || couponCode.isBlank()) {
            return Optional.empty();
        }

        Coupon coupon = couponRepository
                .findByCodeIgnoreCase(couponCode.trim())
                .orElse(null);
        if (coupon == null || !coupon.isActive()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            return Optional.empty();
        }
        if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            return Optional.empty();
        }
        if (coupon.getMaxRedemptions() != null && coupon.getTimesRedeemed() >= coupon.getMaxRedemptions()) {
            return Optional.empty();
        }

        if (coupon.getApplicableCourse() != null) {
            boolean matches = courses.stream()
                    .anyMatch(course -> course.getId().equals(coupon.getApplicableCourse().getId()));
            if (!matches) {
                return Optional.empty();
            }
        }

        return Optional.of(coupon);
    }

    public BigDecimal calculateLineDiscount(BigDecimal baseAmount, Coupon coupon) {
        if (coupon.getPercentOff() > 0) {
            return baseAmount
                    .multiply(BigDecimal.valueOf(coupon.getPercentOff()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (coupon.getAmountOff() != null && coupon.getAmountOff().signum() > 0) {
            return coupon.getAmountOff().min(baseAmount);
        }
        return BigDecimal.ZERO;
    }

    private boolean isCouponApplicableToCourse(Coupon coupon, Course course) {
        if (coupon.getApplicableCourse() == null) {
            return true;
        }
        return coupon.getApplicableCourse().getId().equals(course.getId());
    }

    private String buildCouponLabel(Coupon coupon) {
        if (coupon.getPercentOff() > 0) {
            return coupon.getPercentOff() + "% de descuento";
        }
        if (coupon.getAmountOff() != null && coupon.getAmountOff().signum() > 0) {
            return "USD " + coupon.getAmountOff().stripTrailingZeros().toPlainString() + " de descuento";
        }
        return "Descuento aplicado";
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
