import type { CourseResponse } from '../models/course.model';

export interface PublishBlocker {
  text: string;
  link: string;
}

export interface InstructorProfileForPublish {
  bio?: string | null;
  profilePicture?: string | null;
}

export function hasMultilineContent(text?: string | null): boolean {
  if (!text || typeof text !== 'string') return false;
  return text.split('\n').some((line) => line.trim() !== '');
}

export type ManageSectionKey =
  | 'audience'
  | 'curriculum'
  | 'basics'
  | 'pricing'
  | 'promotions'
  | 'messages';

export function computeSectionsStatus(
  course: CourseResponse | null | undefined,
  opts?: {
    curriculumComplete?: boolean;
    promotionsComplete?: boolean;
  },
): Record<ManageSectionKey, boolean> {
  if (!course) {
    return {
      audience: false,
      curriculum: false,
      basics: false,
      pricing: false,
      promotions: false,
      messages: false,
    };
  }

  const priceNum = Number(course.price);
  const curriculumComplete =
    opts?.curriculumComplete === true ||
    (course.moduleCount ?? 0) > 0 ||
    (course.lessonsCount ?? 0) > 0;

  return {
    audience:
      hasMultilineContent(course.whatYouWillLearn) &&
      hasMultilineContent(course.targetAudience),
    curriculum: curriculumComplete,
    basics: !!(
      course.title?.trim() &&
      course.description?.trim() &&
      course.category &&
      course.category !== '-1' &&
      course.imageUrl?.trim()
    ),
    pricing: !!(
      course.currency?.trim() &&
      Number.isFinite(priceNum) &&
      priceNum >= 0.1
    ),
    promotions: opts?.promotionsComplete ?? false,
    messages:
      Boolean(course.welcomeMessage?.trim()) || Boolean(course.congratulationsMessage?.trim()),
  };
}

function productNoun(courseFormat?: string | null): string {
  const fmt = (courseFormat ?? '').toLowerCase();
  if (fmt === 'ebook') return 'ebook';
  if (fmt === 'podcast') return 'podcast';
  return 'curso';
}

export function getPublishBlockers(
  profile: InstructorProfileForPublish | null | undefined,
  course: CourseResponse | null | undefined,
  manageBasePath: string,
  opts?: { curriculumComplete?: boolean },
): PublishBlocker[] {
  const items: PublishBlocker[] = [];
  if (!course) return items;

  const noun = productNoun(course.courseFormat);

  if (profile) {
    const bioWords = profile.bio
      ? profile.bio
          .trim()
          .split(/\s+/)
          .filter(Boolean).length
      : 0;
    if (bioWords < 50) {
      items.push({
        text: `Biografía del instructor (${bioWords}/50 palabras mín.)`,
        link: '/profile',
      });
    }
    if (!profile.profilePicture?.trim()) {
      items.push({
        text: 'Foto de perfil profesional',
        link: '/profile',
      });
    }
  }

  if (!course.title?.trim()) {
    items.push({ text: `Título del ${noun}`, link: `${manageBasePath}/basics` });
  }
  if (!course.description?.trim()) {
    items.push({ text: `Descripción del ${noun}`, link: `${manageBasePath}/basics` });
  }
  if (!course.category || course.category === '-1') {
    items.push({ text: `Categoría del ${noun}`, link: `${manageBasePath}/basics` });
  }
  if (!course.imageUrl?.trim()) {
    items.push({ text: 'Imagen de portada', link: `${manageBasePath}/basics` });
  }

  if (
    !hasMultilineContent(course.whatYouWillLearn) ||
    !hasMultilineContent(course.targetAudience)
  ) {
    items.push({
      text: `Audiencia y objetivos del ${noun}`,
      link: `${manageBasePath}/audience`,
    });
  }

  const hasCurriculum =
    opts?.curriculumComplete === true ||
    (course.moduleCount ?? 0) > 0 ||
    (course.lessonsCount ?? 0) > 0;
  if (!hasCurriculum) {
    items.push({
      text: 'Diseñar el temario (módulos y lecciones)',
      link: `${manageBasePath}/curriculum`,
    });
  }

  const priceNum = Number(course.price);
  if (!course.currency?.trim() || !Number.isFinite(priceNum) || priceNum < 0.1) {
    items.push({
      text: 'Asignar un precio válido',
      link: `${manageBasePath}/pricing`,
    });
  }

  return items;
}
