import { Injectable, computed, signal } from '@angular/core';
import type { CourseResponse } from '../../shared/models/course.model';
import {
  courseManagePath,
  normalizeCourseSlugForUrl,
  parseCourseSlug,
} from '../../shared/utils/course-slug.util';
import { computeSectionsStatus } from '../../shared/utils/publish-blockers.util';

/** Estado compartido del editor de curso (slug en URL, id numérico para API). */
@Injectable({ providedIn: 'root' })
export class CourseManageContextService {
  readonly courseSlug = signal('');
  readonly courseId = signal<number | null>(null);
  readonly courseFormat = signal<string | null>(null);

  /** Estado de completitud por sección (sidebar). */
  private readonly _sectionStatus = signal<Record<string, boolean>>({
    audience: false,
    curriculum: false,
    basics: false,
    pricing: false,
    promotions: false,
    messages: false,
  });
  readonly sectionStatus = this._sectionStatus.asReadonly();

  private readonly _loadedCourse = signal<CourseResponse | null>(null);
  readonly loadedCourse = this._loadedCourse.asReadonly();

  /** Parámetros de ruta sincronizados al entrar al editor (no dependen del API). */
  private readonly routeFormat = signal<string | null>(null);
  private readonly routeSlugStem = signal<string | null>(null);

  readonly manageBasePath = computed(() => {
    const segments = this.manageNavLink('');
    if (segments.length === 0) {
      return '';
    }
    return '/' + segments.join('/');
  });

  syncRouteParams(format: string | null, slugStem: string | null): void {
    this.routeFormat.set(format?.trim() || null);
    this.routeSlugStem.set(slugStem?.trim() || null);
  }

  manageNavLink(section: string): string[] {
    const format = this.routeFormat();
    const stem = this.routeSlugStem();
    if (format && stem) {
      return section
        ? ['/instructor', 'courses', format, stem, 'manage', section]
        : ['/instructor', 'courses', format, stem, 'manage'];
    }

    if (stem) {
      return section
        ? ['/instructor', 'courses', stem, 'manage', section]
        : ['/instructor', 'courses', stem, 'manage'];
    }

    const slug = this.courseSlug();
    if (slug) {
      const parsed = parseCourseSlug(slug, this.courseFormat());
      if (parsed.format) {
        return section
          ? ['/instructor', 'courses', parsed.format, parsed.slug, 'manage', section]
          : ['/instructor', 'courses', parsed.format, parsed.slug, 'manage'];
      }
      return section
        ? ['/instructor', 'courses', parsed.slug, 'manage', section]
        : ['/instructor', 'courses', parsed.slug, 'manage'];
    }

    return [];
  }

  manageSectionPath(section: string): string {
    const segments = this.manageNavLink(section);
    return segments.length ? '/' + segments.join('/') : '#';
  }

  setCourse(slug: string, id: number, courseFormat?: string | null): void {
    this.courseSlug.set(normalizeCourseSlugForUrl(slug));
    this.courseId.set(id);
    this.courseFormat.set(courseFormat ?? null);
  }

  setSectionStatus(sectionKey: string, complete: boolean): void {
    this._sectionStatus.update((s) => {
      if (s[sectionKey] === complete) {
        return s;
      }
      return { ...s, [sectionKey]: complete };
    });
  }

  patchSectionStatus(patch: Partial<Record<string, boolean>>): void {
    this._sectionStatus.update((s) => {
      let changed = false;
      const next = { ...s };
      for (const [key, value] of Object.entries(patch)) {
        if (value !== undefined && next[key] !== value) {
          next[key] = value;
          changed = true;
        }
      }
      return changed ? next : s;
    });
  }

  setLoadedCourse(course: CourseResponse | null): void {
    this._loadedCourse.set(course);
    if (course) {
      this.syncSectionStatusFromLoadedCourse();
    }
  }

  patchLoadedCourse(patch: Partial<CourseResponse>): void {
    const course = this._loadedCourse();
    if (!course) {
      return;
    }
    let changed = false;
    for (const [key, value] of Object.entries(patch)) {
      if (value === undefined) {
        continue;
      }
      if ((course as unknown as Record<string, unknown>)[key] !== value) {
        changed = true;
        break;
      }
    }
    if (!changed) {
      return;
    }
    this._loadedCourse.set({ ...course, ...patch });
    this.syncSectionStatusFromLoadedCourse();
  }

  /** Recalcula checks del sidebar desde el curso en memoria (p. ej. tras guardar una sección). */
  syncSectionStatusFromLoadedCourse(): void {
    const current = this._sectionStatus();
    const computed = computeSectionsStatus(this._loadedCourse(), {
      curriculumComplete: current['curriculum'],
      promotionsComplete: current['promotions'],
    });
    computed.curriculum = current['curriculum'] || computed.curriculum;

    const keys = Object.keys(computed) as (keyof typeof computed)[];
    if (keys.every((key) => current[key] === computed[key])) {
      return;
    }
    this._sectionStatus.set(computed);
  }

  clear(): void {
    this.courseSlug.set('');
    this.courseId.set(null);
    this.courseFormat.set(null);
    this.routeFormat.set(null);
    this.routeSlugStem.set(null);
    this._loadedCourse.set(null);
    this._sectionStatus.set({
      audience: false,
      curriculum: false,
      basics: false,
      pricing: false,
      promotions: false,
      messages: false,
    });
  }

  requireCourseId(): number {
    const id = this.courseId();
    if (id == null) {
      throw new Error('Curso no cargado en el contexto de gestión.');
    }
    return id;
  }
}
