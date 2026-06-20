import { CourseResponse } from '../models/course.model';
import { mediaBackendUrl } from './media-url.util';
import { MarketplaceCourse } from '../../features/marketplace/models/marketplace-course.model';

export function professorDisplayName(course: CourseResponse): string {
  const professor = course.professor;
  if (professor) {
    const full = [professor.firstName, professor.lastName].filter(Boolean).join(' ').trim();
    if (full) {
      return full;
    }
    if (professor.username?.trim()) {
      return professor.username;
    }
  }
  return 'SkillNet Academy';
}

function formatCourseFormat(course: CourseResponse): string {
  const raw = course.courseFormat?.trim().toLowerCase();
  if (!raw) {
    return 'Curso';
  }
  const map: Record<string, string> = {
    video: 'Curso',
    course: 'Curso',
    ebook: 'Ebook',
    audio: 'Audio',
    membership: 'Membresía',
  };
  return map[raw] ?? 'Curso';
}

export function courseToMarketplace(course: CourseResponse): MarketplaceCourse {
  const original =
    course.originalPrice && course.originalPrice > course.price ? course.originalPrice : undefined;

  return {
    id: course.id,
    title: course.title,
    slug: course.slug,
    description: course.description,
    level: course.level,
    status: course.status,
    price: course.price,
    originalPrice: original,
    category: course.category?.trim() || 'General',
    format: formatCourseFormat(course),
    rating: 4.5,
    enrollmentCount: course.enrollmentCount ?? 0,
    lessonsCount: course.lessonsCount ?? 0,
    moduleCount: course.moduleCount ?? 0,
    professorName: professorDisplayName(course),
    imageUrl: mediaBackendUrl(course.imageUrl) || null,
  };
}

export function courseCoverUrl(course: CourseResponse | null | undefined): string | null {
  if (!course?.imageUrl) {
    return null;
  }
  return mediaBackendUrl(course.imageUrl) || null;
}
