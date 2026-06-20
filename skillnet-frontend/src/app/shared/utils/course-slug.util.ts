/** Slugs alineados con Lernymart: prefijo por formato + stem (ISO-año y variantes). */

export function normalizeCourseSlugForUrl(slug: string): string {
  if (!slug) return slug;
  return slug.replace(/(\d{4,5})(20\d{2})/g, '$1-$2');
}

export function compactCourseSlugForUrl(slug: string): string {
  if (!slug) return slug;
  return slug.replace(/(\d{4,5})-(20\d{2})/g, '$1$2');
}

export function formatSlugPrefix(courseFormat?: string | null): string {
  const format = (courseFormat ?? 'course').trim().toLowerCase();
  const map: Record<string, string> = {
    course: 'curso',
    videocourse: 'curso',
    video: 'curso',
    ebook: 'ebook',
    podcast: 'podcast',
    audiobook: 'audiolibro',
    workshop: 'taller',
    subscription: 'suscripcion',
    event: 'evento',
    app: 'app',
    script: 'script',
    image: 'imagen',
  };
  return map[format] ?? format;
}

export interface ParsedCourseSlug {
  format: string;
  slug: string;
  full: string;
}

export function parseCourseSlug(fullSlug: string, courseFormat?: string | null): ParsedCourseSlug {
  const normalized = normalizeCourseSlugForUrl(fullSlug);
  const slash = normalized.indexOf('/');
  if (slash > 0) {
    return {
      format: normalized.slice(0, slash),
      slug: normalized.slice(slash + 1),
      full: normalized,
    };
  }
  if (courseFormat) {
    const format = formatSlugPrefix(courseFormat);
    return { format, slug: normalized, full: `${format}/${normalized}` };
  }
  return { format: '', slug: normalized, full: normalized };
}

export function joinCourseSlug(format: string, slug: string): string {
  if (!slug) return format;
  if (slug.includes('/')) return normalizeCourseSlugForUrl(slug);
  return `${format}/${slug}`;
}

export function courseSlugLookupCandidates(slug: string): string[] {
  if (!slug) return [];
  const normalized = normalizeCourseSlugForUrl(slug);
  const compact = compactCourseSlugForUrl(normalized);
  const parsed = parseCourseSlug(normalized);
  return [...new Set([slug, normalized, compact, parsed.full, parsed.slug])];
}

export function slugifyCourseTitle(title: string): string {
  if (!title?.trim()) return 'producto';
  const prepared = title.trim().replace(/(\d{4,5})\s*:\s*(20\d{2})\b/g, '$1-$2');
  const slug = prepared
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 100);
  return normalizeCourseSlugForUrl(slug || 'producto');
}

function courseMarketplaceSegments(
  fullSlug: string,
  courseFormat?: string | null,
): { format: string; slug: string } {
  const { format, slug } = parseCourseSlug(fullSlug, courseFormat);
  return { format, slug };
}

export function courseLearnPath(fullSlug: string, courseFormat?: string | null): string {
  const { format, slug } = courseMarketplaceSegments(fullSlug, courseFormat);
  return format
    ? `/marketplace/course/${format}/${slug}/learn`
    : `/marketplace/course/${slug}/learn`;
}

export function courseLandingPath(fullSlug: string, courseFormat?: string | null): string {
  const { format, slug } = courseMarketplaceSegments(fullSlug, courseFormat);
  return format ? `/marketplace/course/${format}/${slug}` : `/marketplace/course/${slug}`;
}

export function courseLandingRouterLink(
  fullSlug: string,
  courseFormat?: string | null,
): (string | number)[] {
  const { format, slug } = courseMarketplaceSegments(fullSlug, courseFormat);
  return format ? ['/marketplace/course', format, slug] : ['/marketplace/course', slug];
}

export function courseLearnRouterLink(
  fullSlug: string,
  courseFormat?: string | null,
): (string | number)[] {
  const { format, slug } = courseMarketplaceSegments(fullSlug, courseFormat);
  return format
    ? ['/marketplace/course', format, slug, 'learn']
    : ['/marketplace/course', slug, 'learn'];
}

export function slugFromRouteParams(format: string | null, slug: string | null): string {
  if (format && slug) {
    return joinCourseSlug(format, slug);
  }
  return slug ?? '';
}

export function studentCourseApiSegments(
  fullSlug: string,
  courseFormat?: string | null,
): string[] {
  const { format, slug } = courseMarketplaceSegments(fullSlug, courseFormat);
  if (format && slug) {
    return [format, slug];
  }
  return [fullSlug];
}

export function studentCourseApiPath(fullSlug: string, courseFormat?: string | null): string {
  const segments = studentCourseApiSegments(fullSlug, courseFormat);
  return segments.map((segment) => encodeURIComponent(segment)).join('/');
}

export function courseManagePath(fullSlug: string, section?: string, courseFormat?: string | null): string {
  const { format, slug } = parseCourseSlug(fullSlug, courseFormat);
  const base = format
    ? `/instructor/courses/${format}/${slug}/manage`
    : `/instructor/courses/${slug}/manage`;
  return section ? `${base}/${section}` : base;
}

export function courseRouteNeedsRedirect(
  formatParam: string | null,
  slugParam: string | null,
  canonicalSlug: string,
  courseFormat?: string | null,
): boolean {
  const expected = parseCourseSlug(canonicalSlug, courseFormat);
  if (expected.format) {
    return formatParam !== expected.format || slugParam !== expected.slug;
  }
  return Boolean(formatParam) || slugParam !== expected.slug;
}
