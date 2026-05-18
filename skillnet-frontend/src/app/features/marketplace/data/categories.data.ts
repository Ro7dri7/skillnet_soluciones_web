export const MARKETPLACE_CATEGORIES = [
  'Tecnología',
  'Finanzas y Negocios',
  'Marketing',
  'Diseño',
  'Desarrollo Personal',
] as const;

export type MarketplaceCategory = (typeof MARKETPLACE_CATEGORIES)[number];

export const OFFER_FORMATS = [
  { title: 'Cursos', image: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400' },
  { title: 'E-books', image: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400' },
  { title: 'Talleres', image: 'https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=400' },
  { title: 'Software', image: 'https://images.unsplash.com/photo-1551650975-87deedd944c3?w=400' },
  { title: 'Audiolibros', image: 'https://images.unsplash.com/photo-1478145046317-39f10e56b5e9?w=400' },
  { title: 'Eventos', image: 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=400' },
] as const;
