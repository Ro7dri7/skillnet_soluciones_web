import type { ProductType } from '../../../shared/models/course-builder.model';

export interface ProductTypeOption {
  id: ProductType;
  label: string;
  image?: string;
  comingSoon?: boolean;
}

export const PRODUCT_TYPES: ProductTypeOption[] = [
  {
    id: 'course',
    label: 'Curso',
    image:
      'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?q=80&w=2671&auto=format&fit=crop',
  },
  {
    id: 'audiobook',
    label: 'Audiolibro',
    image:
      'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?q=80&w=2670&auto=format&fit=crop',
    comingSoon: true,
  },
  {
    id: 'ebook',
    label: 'E-book',
    image:
      'https://images.unsplash.com/photo-1589998059171-988d887df646?q=80&w=2670&auto=format&fit=crop',
  },
  {
    id: 'subscription',
    label: 'Suscripción',
    image:
      'https://images.unsplash.com/photo-1563013544-824ae1b704d3?q=80&w=2670&auto=format&fit=crop',
    comingSoon: true,
  },
  {
    id: 'event',
    label: 'Evento',
    image:
      'https://images.unsplash.com/photo-1475721027785-f74dea327912?q=80&w=2670&auto=format&fit=crop',
    comingSoon: true,
  },
  {
    id: 'workshop',
    label: 'Talleres',
    image:
      'https://images.unsplash.com/photo-1558403194-611308249627?q=80&w=2670&auto=format&fit=crop',
    comingSoon: true,
  },
  {
    id: 'app',
    label: 'Aplicación',
    image:
      'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?q=80&w=2574&auto=format&fit=crop',
    comingSoon: true,
  },
  {
    id: 'script',
    label: 'Script / Automatización',
    image:
      'https://images.unsplash.com/photo-1555066931-4365d14bab8c?q=80&w=2670&auto=format&fit=crop',
    comingSoon: true,
  },
  {
    id: 'image',
    label: 'Imagen',
    image:
      'https://images.unsplash.com/photo-1542038784456-1ea8e935640e?q=80&w=2670&auto=format&fit=crop',
    comingSoon: true,
  },
  {
    id: 'podcast',
    label: 'Podcast',
    image:
      'https://images.unsplash.com/photo-1478737270239-2f02b77fc618?q=80&w=2574&auto=format&fit=crop',
  },
];
