/** Categorías oficiales (misma estructura que LernyMart). */
export const OFFICIAL_CATEGORIES = [
  'Finanzas y Negocios',
  'Gestión y Operaciones',
  'Marketing y Ventas',
  'Tecnología y Data',
  'Desarrollo Profesional',
  'Creatividad y Diseño',
] as const;

export type OfficialCategory = (typeof OFFICIAL_CATEGORIES)[number];

/** Alias usado por carruseles y catálogo. */
export const MARKETPLACE_CATEGORIES = OFFICIAL_CATEGORIES;

export const SUBCATEGORIES: Record<OfficialCategory, readonly string[]> = {
  'Finanzas y Negocios': [
    'Contabilidad',
    'Finanzas',
    'Inversiones',
    'Emprendimiento',
    'Administración',
  ],
  'Gestión y Operaciones': [
    'Gestión de proyectos',
    'Productividad',
    'Gestión de operaciones',
    'Gestión de procesos',
    'Gestión de calidad',
  ],
  'Marketing y Ventas': [
    'Marketing',
    'Marketing digital',
    'Trade marketing',
    'Branding',
    'Ventas',
    'E-commerce',
    'Gestión comercial',
    'Experiencia al cliente',
    'Redes sociales',
  ],
  'Tecnología y Data': [
    'Programación',
    'Desarrollo de software',
    'Desarrollo web',
    'Data analytics',
    'Machine learning',
    'Informática',
    'Inteligencia artificial',
    'Automatización',
    'Transformación digital',
  ],
  'Desarrollo Profesional': [
    'Liderazgo',
    'Mindset',
    'Habilidades blandas',
    'People management',
  ],
  'Creatividad y Diseño': [
    'Diseño gráfico',
    'Creatividad aplicada',
    'UX/UI',
    'Producto digital',
    'Fotografía y video',
  ],
};

const subToParentMap: Record<string, OfficialCategory> = {};
for (const [parent, subs] of Object.entries(SUBCATEGORIES) as [OfficialCategory, readonly string[]][]) {
  for (const sub of subs) {
    subToParentMap[sub] = parent;
  }
}

export function isOfficialCategory(value: string): value is OfficialCategory {
  return (OFFICIAL_CATEGORIES as readonly string[]).includes(value);
}

export function getParentCategory(value: string): OfficialCategory | null {
  if (isOfficialCategory(value)) {
    return value;
  }
  return subToParentMap[value] ?? null;
}

export function getSubcategories(category: string): readonly string[] {
  if (isOfficialCategory(category)) {
    return SUBCATEGORIES[category];
  }
  return [];
}

export const OFFICIAL_LEVELS = [
  { value: 'beginner', label: 'Principiante' },
  { value: 'intermediate', label: 'Intermedio' },
  { value: 'advanced', label: 'Avanzado' },
  { value: 'expert', label: 'Experto' },
] as const;

export const OFFICIAL_LANGUAGES = [
  { value: 'en', label: 'Inglés' },
  { value: 'es', label: 'Español' },
  { value: 'pt', label: 'Portugués' },
] as const;

export const OFFICIAL_CURRENCIES = [
  { value: 'USD', label: 'USD' },
  { value: 'EUR', label: 'EUR' },
  { value: 'PEN', label: 'PEN' },
  { value: 'COP', label: 'COP' },
  { value: 'MXN', label: 'MXN' },
  { value: 'CLP', label: 'CLP' },
  { value: 'BRL', label: 'BRL' },
  { value: 'ARS', label: 'ARS' },
] as const;

export const OFFER_FORMATS = [
  { title: 'Cursos', image: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400' },
  { title: 'E-books', image: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400' },
  { title: 'Talleres', image: 'https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=400' },
  { title: 'Software', image: 'https://images.unsplash.com/photo-1551650975-87deedd944c3?w=400' },
  { title: 'Audiolibros', image: 'https://images.unsplash.com/photo-1478145046317-39f10e56b5e9?w=400' },
  { title: 'Eventos', image: 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=400' },
] as const;
