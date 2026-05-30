package com.skillnet.service.admin;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.PaymentRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.persistence.repository.projection.DailyRevenueProjection;
import com.skillnet.web.dto.response.AdminChartPointDTO;
import com.skillnet.web.dto.response.AdminDashboardResponseDTO;
import com.skillnet.web.dto.response.AdminKpiDTO;
import com.skillnet.web.dto.response.AdminTopProducerDTO;
import com.skillnet.web.dto.response.AdminTransactionDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter RANGE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("es"));

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponseDTO getDashboard(
            String period, String view, LocalDate customStart, LocalDate customEnd) {
        PeriodWindow window = resolveWindow(period, customStart, customEnd);
        PeriodWindow previous = window.previous();

        String normalizedView = normalizeView(view);
        Metrics current = loadMetrics(window);
        Metrics prev = loadMetrics(previous);

        return AdminDashboardResponseDTO.builder()
                .periodLabel(window.label())
                .periodStart(window.start().toString())
                .periodEnd(window.endExclusive().minus(1, ChronoUnit.DAYS).toString())
                .view(normalizedView)
                .kpis(buildKpis(normalizedView, current, prev))
                .revenueSeries(buildRevenueSeries(window, previous))
                .usersSeries(buildUsersSeries(window, previous))
                .recentTransactions(loadRecentTransactions())
                .topProducers(loadTopProducers(window))
                .pendingDraftCourses(courseRepository.countByStatus("draft"))
                .inactiveUsersTotal(userRepository.countByActiveFalse())
                .build();
    }

    private String normalizeView(String view) {
        if (view == null || view.isBlank()) {
            return "resumen";
        }
        return view.trim().toLowerCase(Locale.ROOT);
    }

    private Metrics loadMetrics(PeriodWindow window) {
        Instant start = window.startInstant();
        Instant end = window.endInstant();
        BigDecimal revenue = paymentRepository.sumCompletedAmountBetween(start, end);
        long sales = paymentRepository.countCompletedBetween(start, end);
        long newUsers = userRepository.countByDateJoinedGreaterThanEqualAndDateJoinedLessThan(start, end);
        long deactivated = userRepository.countByDateJoinedGreaterThanEqualAndDateJoinedLessThanAndActiveFalse(start, end);
        long enrollments = enrollmentRepository.countByEnrolledAtGreaterThanEqualAndEnrolledAtLessThan(start, end);
        long graduates = enrollmentRepository.countByCompletedTrueAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(start, end);
        long newCourses = courseRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        BigDecimal avgTicket = sales > 0
                ? revenue.divide(BigDecimal.valueOf(sales), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new Metrics(revenue, sales, newUsers, deactivated, enrollments, graduates, newCourses, avgTicket);
    }

    private List<AdminKpiDTO> buildKpis(String view, Metrics current, Metrics previous) {
        return switch (view) {
            case "ventas" -> List.of(
                    kpi("revenue", "Ingresos netos", money(current.revenue()), pct(current.revenue(), previous.revenue()), "#032B60", "ri-coin-line"),
                    kpi("sales", "Transacciones", num(current.sales()), pctLong(current.sales(), previous.sales()), "#145bff", "ri-shopping-cart-line"),
                    kpi("ticket", "Ticket promedio", money(current.avgTicket()), pct(current.avgTicket(), previous.avgTicket()), "#F59E0B", "ri-receipt-line"),
                    kpi("enrollments", "Matrículas", num(current.enrollments()), pctLong(current.enrollments(), previous.enrollments()), "#39b8fd", "ri-book-open-line"));
            case "cursos" -> List.of(
                    kpi("newCourses", "Cursos nuevos", num(current.newCourses()), pctLong(current.newCourses(), previous.newCourses()), "#145bff", "ri-book-2-line"),
                    kpi("sales", "Ventas de cursos", num(current.sales()), pctLong(current.sales(), previous.sales()), "#032B60", "ri-shopping-bag-line"),
                    kpi("revenue", "Ingresos", money(current.revenue()), pct(current.revenue(), previous.revenue()), "#39b8fd", "ri-coin-line"),
                    kpi("drafts", "Borradores pendientes", num(courseRepository.countByStatus("draft")), "0", "#6366f1", "ri-draft-line"));
            case "usuarios" -> List.of(
                    kpi("newUsers", "Nuevos usuarios", num(current.newUsers()), pctLong(current.newUsers(), previous.newUsers()), "#00A87E", "ri-user-add-line"),
                    kpi("deactivated", "Bajas / inactivos", num(current.deactivated()), pctLong(current.deactivated(), previous.deactivated()), "#E84545", "ri-user-unfollow-line"),
                    kpi("enrollments", "Matrículas", num(current.enrollments()), pctLong(current.enrollments(), previous.enrollments()), "#145bff", "ri-graduation-cap-line"),
                    kpi("totalInactive", "Inactivos totales", num(userRepository.countByActiveFalse()), "0", "#8099BC", "ri-user-forbid-line"));
            case "titulados" -> List.of(
                    kpi("graduates", "Titulados", num(current.graduates()), pctLong(current.graduates(), previous.graduates()), "#00A87E", "ri-award-line"),
                    kpi("completed", "Completaron curso", num(current.graduates()), pctLong(current.graduates(), previous.graduates()), "#145bff", "ri-medal-line"),
                    kpi("enrollments", "Matrículas", num(current.enrollments()), pctLong(current.enrollments(), previous.enrollments()), "#032B60", "ri-book-open-line"),
                    kpi("rate", "Tasa titulación", rate(current.graduates(), current.enrollments()), "0", "#39b8fd", "ri-percent-line"));
            default -> List.of(
                    kpi("revenue", "Ingresos netos", money(current.revenue()), pct(current.revenue(), previous.revenue()), "#032B60", "ri-coin-line"),
                    kpi("newUsers", "Nuevos usuarios", num(current.newUsers()), pctLong(current.newUsers(), previous.newUsers()), "#00A87E", "ri-user-add-line"),
                    kpi("sales", "Cursos vendidos", num(current.sales()), pctLong(current.sales(), previous.sales()), "#145bff", "ri-shopping-cart-line"),
                    kpi("graduates", "Titulados", num(current.graduates()), pctLong(current.graduates(), previous.graduates()), "#39b8fd", "ri-award-line"),
                    kpi("deactivated", "Dados de baja", num(current.deactivated()), pctLong(current.deactivated(), previous.deactivated()), "#E84545", "ri-user-unfollow-line"),
                    kpi("ticket", "Ticket promedio", money(current.avgTicket()), pct(current.avgTicket(), previous.avgTicket()), "#F59E0B", "ri-receipt-line"));
        };
    }

    private AdminKpiDTO kpi(
            String id, String label, String value, String change, String color, String icon) {
        String direction = "neutral";
        if (change.startsWith("+")) {
            direction = "up";
        } else if (change.startsWith("-")) {
            direction = "down";
        }
        return AdminKpiDTO.builder()
                .id(id)
                .label(label)
                .value(value)
                .changePercent(change)
                .changeDirection(direction)
                .meta("vs período anterior")
                .accentColor(color)
                .icon(icon)
                .build();
    }

    private List<AdminChartPointDTO> buildRevenueSeries(PeriodWindow current, PeriodWindow previous) {
        Map<LocalDate, BigDecimal> cur = toDailyMap(paymentRepository.sumDailyRevenuePlatform(current.startInstant(), current.endInstant()));
        Map<LocalDate, BigDecimal> prev = toDailyMap(paymentRepository.sumDailyRevenuePlatform(previous.startInstant(), previous.endInstant()));
        List<LocalDate> days = current.days();
        List<AdminChartPointDTO> points = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) {
            LocalDate day = days.get(i);
            LocalDate prevDay = i < previous.days().size() ? previous.days().get(i) : null;
            points.add(AdminChartPointDTO.builder()
                    .label(shortDayLabel(day))
                    .current(cur.getOrDefault(day, BigDecimal.ZERO).doubleValue())
                    .previous(prevDay != null ? prev.getOrDefault(prevDay, BigDecimal.ZERO).doubleValue() : 0)
                    .build());
        }
        return points;
    }

    private List<AdminChartPointDTO> buildUsersSeries(PeriodWindow current, PeriodWindow previous) {
        List<AdminChartPointDTO> points = new ArrayList<>();
        for (int i = 0; i < current.days().size(); i++) {
            LocalDate day = current.days().get(i);
            Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            long count = userRepository.countByDateJoinedGreaterThanEqualAndDateJoinedLessThan(start, end);
            LocalDate prevDay = i < previous.days().size() ? previous.days().get(i) : null;
            long prevCount = 0;
            if (prevDay != null) {
                Instant pStart = prevDay.atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant pEnd = prevDay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                prevCount = userRepository.countByDateJoinedGreaterThanEqualAndDateJoinedLessThan(pStart, pEnd);
            }
            points.add(AdminChartPointDTO.builder()
                    .label(shortDayLabel(day))
                    .current(count)
                    .previous(prevCount)
                    .build());
        }
        return points;
    }

    private Map<LocalDate, BigDecimal> toDailyMap(List<DailyRevenueProjection> rows) {
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (DailyRevenueProjection row : rows) {
            if (row.getDate() != null) {
                map.put(row.getDate(), row.getAmount() != null ? row.getAmount() : BigDecimal.ZERO);
            }
        }
        return map;
    }

    private List<AdminTransactionDTO> loadRecentTransactions() {
        return paymentRepository.findRecentWithDetails(PageRequest.of(0, 8)).stream()
                .map(this::toTransaction)
                .toList();
    }

    private AdminTransactionDTO toTransaction(Payment payment) {
        User user = payment.getUser();
        Course course = payment.getCourse();
        String name = formatName(user);
        return AdminTransactionDTO.builder()
                .id(payment.getId())
                .buyerName(name)
                .buyerInitials(initials(name))
                .courseTitle(course != null ? course.getTitle() : "—")
                .amount(payment.getAmount())
                .status(mapPaymentStatus(payment.getStatus()))
                .build();
    }

    private List<AdminTopProducerDTO> loadTopProducers(PeriodWindow window) {
        List<Object[]> rows = paymentRepository.sumRevenueGroupedByProfessor(window.startInstant(), window.endInstant());
        List<AdminTopProducerDTO> result = new ArrayList<>();
        for (Object[] row : rows.stream().limit(5).toList()) {
            Long professorId = (Long) row[0];
            BigDecimal revenue = row[1] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            userRepository.findById(professorId).ifPresent(professor -> {
                String category = courseRepository.findByProfessor_Id(professorId).stream()
                        .map(Course::getCategory)
                        .filter(c -> c != null && !c.isBlank())
                        .findFirst()
                        .orElse("General");
                result.add(AdminTopProducerDTO.builder()
                        .id(professorId)
                        .name(formatName(professor))
                        .category(category)
                        .revenue(revenue)
                        .build());
            });
        }
        return result;
    }

    private String formatName(User user) {
        if (user == null) {
            return "Usuario";
        }
        String full = String.join(" ",
                        user.getFirstName() != null ? user.getFirstName().trim() : "",
                        user.getLastName() != null ? user.getLastName().trim() : "")
                .trim();
        return full.isBlank() ? user.getUsername() : full;
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    private String mapPaymentStatus(String status) {
        if (status == null) {
            return "Pendiente";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "COMPLETED", "SUCCEEDED" -> "Completado";
            case "FAILED", "CANCELLED" -> "Fallido";
            default -> "Pendiente";
        };
    }

    private String money(BigDecimal value) {
        return "$" + (value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
    }

    private String num(long value) {
        return String.valueOf(value);
    }

    private String rate(long part, long total) {
        if (total <= 0) {
            return "0%";
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private String pct(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        BigDecimal diff = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
        return (diff.signum() >= 0 ? "+" : "") + diff + "%";
    }

    private String pctLong(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double diff = (current - previous) * 100.0 / previous;
        return (diff >= 0 ? "+" : "") + BigDecimal.valueOf(diff).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private String shortDayLabel(LocalDate day) {
        return day.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, new Locale("es"));
    }

    private PeriodWindow resolveWindow(String period, LocalDate customStart, LocalDate customEnd) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String p = period == null ? "semana" : period.toLowerCase(Locale.ROOT);
        return switch (p) {
            case "dia", "day" -> PeriodWindow.day(today);
            case "mes", "month" -> PeriodWindow.month(today);
            case "anio", "year" -> PeriodWindow.year(today);
            case "personalizado", "custom" -> PeriodWindow.custom(customStart, customEnd);
            default -> PeriodWindow.week(today);
        };
    }

    private record Metrics(
            BigDecimal revenue,
            long sales,
            long newUsers,
            long deactivated,
            long enrollments,
            long graduates,
            long newCourses,
            BigDecimal avgTicket) {}

    private record PeriodWindow(LocalDate start, LocalDate endExclusive, String label) {
        static PeriodWindow day(LocalDate day) {
            return new PeriodWindow(day, day.plusDays(1), "Hoy · " + day.format(RANGE_FMT));
        }

        static PeriodWindow week(LocalDate ref) {
            LocalDate start = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate end = start.plusWeeks(1);
            int week = start.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            String label = "Sem " + week + " · " + start.format(RANGE_FMT) + " – "
                    + end.minusDays(1).format(RANGE_FMT);
            return new PeriodWindow(start, end, label);
        }

        static PeriodWindow month(LocalDate ref) {
            LocalDate start = ref.withDayOfMonth(1);
            LocalDate end = start.plusMonths(1);
            return new PeriodWindow(start, end, ref.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("es")) + " " + ref.getYear());
        }

        static PeriodWindow year(LocalDate ref) {
            LocalDate start = ref.withDayOfYear(1);
            LocalDate end = start.plusYears(1);
            return new PeriodWindow(start, end, "Año " + ref.getYear());
        }

        static PeriodWindow custom(LocalDate start, LocalDate end) {
            if (start == null || end == null || end.isBefore(start)) {
                return week(LocalDate.now(ZoneOffset.UTC));
            }
            return new PeriodWindow(start, end.plusDays(1), start.format(RANGE_FMT) + " – " + end.format(RANGE_FMT));
        }

        Instant startInstant() {
            return start.atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        Instant endInstant() {
            return endExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        List<LocalDate> days() {
            List<LocalDate> list = new ArrayList<>();
            for (LocalDate d = start; d.isBefore(endExclusive); d = d.plusDays(1)) {
                list.add(d);
            }
            return list;
        }

        PeriodWindow previous() {
            long len = ChronoUnit.DAYS.between(start, endExclusive);
            LocalDate prevEnd = start;
            LocalDate prevStart = prevEnd.minusDays(len);
            return new PeriodWindow(prevStart, prevEnd, "Período anterior");
        }
    }
}
