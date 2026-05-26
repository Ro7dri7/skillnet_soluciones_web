export interface ManageNavItem {
  label: string;
  path: string;
  icon: string;
  sectionKey: string;
}

export interface ManageNavSection {
  title: string;
  items: ManageNavItem[];
}

export const MANAGE_NAV_SECTIONS: ManageNavSection[] = [
  {
    title: 'PLANIFICA TU CURSO',
    items: [
      {
        label: 'Define tu audiencia',
        path: 'audience',
        icon: 'ri-group-line',
        sectionKey: 'audience',
      },
    ],
  },
  {
    title: 'CREA TU CONTENIDO',
    items: [
      {
        label: 'Diseña tu temario',
        path: 'curriculum',
        icon: 'ri-list-check',
        sectionKey: 'curriculum',
      },
    ],
  },
  {
    title: 'PUBLICA TU CURSO',
    items: [
      {
        label: 'Página de inicio',
        path: 'basics',
        icon: 'ri-layout-top-line',
        sectionKey: 'basics',
      },
      {
        label: 'Precios',
        path: 'pricing',
        icon: 'ri-price-tag-3-line',
        sectionKey: 'pricing',
      },
      {
        label: 'Promociones',
        path: 'promotions',
        icon: 'ri-percent-line',
        sectionKey: 'promotions',
      },
      {
        label: 'Mensajes',
        path: 'messages',
        icon: 'ri-chat-voice-line',
        sectionKey: 'messages',
      },
    ],
  },
];
