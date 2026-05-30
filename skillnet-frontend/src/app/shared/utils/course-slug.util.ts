/** Slugs alineados con Lernymart (ISO-año y variantes de búsqueda). */

export function normalizeCourseSlugForUrl(slug: string): string {
  if (!slug) return slug;
  return slug.replace(/(\d{4,5})(20\d{2})/g, '$1-$2');
}

export function compactCourseSlugForUrl(slug: string): string {
  if (!slug) return slug;
  return slug.replace(/(\d{4,5})-(20\d{2})/g, '$1$2');
}

export function courseSlugLookupCandidates(slug: string): string[] {
  if (!slug) return [];
  const normalized = normalizeCourseSlugForUrl(slug);
  const compact = compactCourseSlugForUrl(normalized);
  return [...new Set([slug, normalized, compact])];
}

export function slugifyCourseTitle(title: string): string {
  if (!title?.trim()) return 'curso';
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
  return normalizeCourseSlugForUrl(slug || 'curso');
}

export function courseLearnPath(slug: string): string {
  return `/marketplace/course/${normalizeCourseSlugForUrl(slug)}/learn`;
}

export function courseLandingPath(slug: string): string {
  return `/marketplace/course/${normalizeCourseSlugForUrl(slug)}`;
}
