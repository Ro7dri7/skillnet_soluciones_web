package com.skillnet.util;

import com.skillnet.domain.CourseFormat;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.repository.CourseRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Slugs de curso alineados con Lernymart (prefijo por formato, ISO-año, variantes de búsqueda). */
public final class CourseSlugUtils {

    private static final Pattern ISO_COLON_YEAR = Pattern.compile("(\\d{4,5})\\s*:\\s*(20\\d{2})\\b");
    private static final Pattern ISO_YEAR_IN_SLUG = Pattern.compile("(\\d{4,5})(20\\d{2})");
    private static final Pattern ISO_YEAR_HYPHEN = Pattern.compile("(\\d{4,5})-(20\\d{2})");

    private CourseSlugUtils() {}

    public static String slugifyCourseTitle(String title) {
        if (title == null || title.isBlank()) {
            return "curso";
        }
        String prepared = ISO_COLON_YEAR.matcher(title.trim()).replaceAll("$1-$2");
        String normalized = Normalizer.normalize(prepared, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        String slug = normalized
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "curso";
        }
        if (slug.length() > 100) {
            slug = slug.substring(0, 100).replaceAll("-+$", "");
        }
        return normalizeIsoYearInSlug(slug);
    }

    public static String normalizeIsoYearInSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return slug;
        }
        return ISO_YEAR_IN_SLUG.matcher(slug).replaceAll("$1-$2");
    }

    public static String compactIsoYearInSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return slug;
        }
        return ISO_YEAR_HYPHEN.matcher(slug).replaceAll("$1$2");
    }

    public static String formatSlugPrefix(String courseFormat) {
        return switch (CourseFormat.fromDbValue(courseFormat)) {
            case VIDEOCOURSE -> "curso";
            case EBOOK -> "ebook";
            case PODCAST -> "podcast";
            case AUDIOBOOK -> "audiolibro";
            case WORKSHOP -> "taller";
            case SUBSCRIPTION -> "suscripcion";
            case EVENT -> "evento";
            case APP -> "app";
            case SCRIPT -> "script";
            case IMAGE -> "imagen";
        };
    }

    public static String buildPrefixedSlug(String courseFormat, String titleSlug) {
        String prefix = formatSlugPrefix(courseFormat);
        String stem = titleSlug == null || titleSlug.isBlank() ? "producto" : titleSlug;
        return prefix + "/" + stem;
    }

    public static String joinRouteSlug(String formatSegment, String slugSegment) {
        if (formatSegment == null || formatSegment.isBlank()) {
            return slugSegment;
        }
        if (slugSegment == null || slugSegment.isBlank()) {
            return formatSegment;
        }
        if (slugSegment.contains("/")) {
            return slugSegment;
        }
        return formatSegment + "/" + slugSegment;
    }

    public static List<String> lookupVariants(String slug) {
        if (slug == null || slug.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        variants.add(slug);
        String normalized = normalizeIsoYearInSlug(slug);
        variants.add(normalized);
        variants.add(compactIsoYearInSlug(normalized));
        int slash = normalized.indexOf('/');
        if (slash > 0) {
            variants.add(normalized.substring(slash + 1));
        } else {
            variants.add(buildPrefixedSlug(CourseFormat.VIDEOCOURSE.getDbValue(), normalized));
        }
        return new ArrayList<>(variants);
    }

    public static Optional<Course> resolveCourse(CourseRepository repository, String slug) {
        for (String candidate : lookupVariants(slug)) {
            Optional<Course> hit = repository.findBySlug(candidate);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }

    public static String uniqueSlug(
            CourseRepository repository, String title, String courseFormat, Long excludeCourseId) {
        String base = buildPrefixedSlug(courseFormat, slugifyCourseTitle(title));
        String candidate = base;
        int suffix = 2;
        while (slugExists(repository, candidate, excludeCourseId)) {
            int slash = base.lastIndexOf('/');
            String prefix = slash > 0 ? base.substring(0, slash + 1) : "";
            String stem = slash > 0 ? base.substring(slash + 1) : base;
            candidate = prefix + stem + "-" + suffix++;
        }
        return candidate;
    }

    public static String uniqueSlug(CourseRepository repository, String title, Long excludeCourseId) {
        return uniqueSlug(repository, title, CourseFormat.VIDEOCOURSE.getDbValue(), excludeCourseId);
    }

    private static boolean slugExists(CourseRepository repository, String slug, Long excludeCourseId) {
        if (excludeCourseId == null) {
            return repository.existsBySlug(slug);
        }
        return repository.existsBySlugAndIdNot(slug, excludeCourseId);
    }
}
