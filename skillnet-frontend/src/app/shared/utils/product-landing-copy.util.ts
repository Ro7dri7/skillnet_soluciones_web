export interface ProductLandingCopy {
  productNoun: string;
  instructorLabel: string;
  advantagesTitle: string;
  advantages: string[];
  goalsSectionTitle: string;
  goalsEmptyText: string;
  audienceSectionTitle: string;
  audienceEmptyText: string;
  requirementsSectionTitle: string;
  requirementsEmptyText: string;
  curriculumSectionTitle: string;
  showCurriculum: boolean;
  showStudentCount: boolean;
  durationHeroLabel: string;
}

const COURSE_COPY: ProductLandingCopy = {
  productNoun: 'curso',
  instructorLabel: 'Infoproductor',
  advantagesTitle: 'Ventajas de tomar el curso',
  advantages: [
    'Garantía de 7 días',
    'Estudia a tu manera y en cualquier dispositivo',
    '92% de evaluaciones positivas',
    '+4100 estudiantes',
  ],
  goalsSectionTitle: 'Al finalizar el curso:',
  goalsEmptyText: 'Se detallarán las metas alcanzadas durante el curso.',
  audienceSectionTitle: 'Curso ideal para:',
  audienceEmptyText: 'Profesionales y estudiantes entusiastas en el área.',
  requirementsSectionTitle: '¿Qué necesitan para aprender?',
  requirementsEmptyText: 'No se requieren conocimientos previos para este curso.',
  curriculumSectionTitle: 'Este curso incluye:',
  showCurriculum: true,
  showStudentCount: true,
  durationHeroLabel: 'Duración',
};

const EBOOK_COPY: ProductLandingCopy = {
  ...COURSE_COPY,
  productNoun: 'ebook',
  advantagesTitle: 'Ventajas de este ebook',
  advantages: [
    'Garantía de 7 días',
    'Lee en cualquier dispositivo',
    'Acceso inmediato tras la compra',
    'Contenido descargable para consultar cuando quieras',
  ],
  goalsSectionTitle: 'Al terminar de leer:',
  audienceSectionTitle: 'Lector ideal para:',
  requirementsSectionTitle: '¿Qué necesitas?',
  requirementsEmptyText: 'No se requieren conocimientos previos.',
  curriculumSectionTitle: 'Este ebook incluye:',
  showCurriculum: false,
  showStudentCount: false,
  durationHeroLabel: 'Extensión',
};

const PODCAST_COPY: ProductLandingCopy = {
  ...COURSE_COPY,
  productNoun: 'podcast',
  instructorLabel: 'Creador',
  advantagesTitle: 'Ventajas de este podcast',
  advantages: [
    'Garantía de 7 días',
    'Escucha donde quieras',
    'Acceso inmediato tras la compra',
    'Episodios listos para consumir a tu ritmo',
  ],
  goalsSectionTitle: 'Al escuchar:',
  audienceSectionTitle: 'Oyente ideal para:',
  requirementsSectionTitle: '¿Qué necesitas?',
  curriculumSectionTitle: 'Este podcast incluye:',
  showCurriculum: false,
  showStudentCount: false,
  durationHeroLabel: 'Duración total',
};

export function getProductLandingCopy(courseFormat?: string | null): ProductLandingCopy {
  const fmt = (courseFormat ?? '').toLowerCase().trim();
  if (fmt === 'ebook') {
    return EBOOK_COPY;
  }
  if (fmt === 'podcast' || fmt === 'audio') {
    return PODCAST_COPY;
  }
  return COURSE_COPY;
}

export function parseMultilineList(text: string | null | undefined): string[] {
  if (!text?.trim()) {
    return [];
  }
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
}
