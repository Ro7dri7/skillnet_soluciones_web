export interface BuilderShellNavItem {
  label: string;
  wizardPath: string;
  managePath: string;
  sectionKey: string;
}

export interface BuilderShellNavSection {
  title: string;
  items: BuilderShellNavItem[];
}

export const BUILDER_SHELL_NAV: BuilderShellNavSection[] = [
  {
    title: 'Planifica tu curso',
    items: [
      {
        label: 'Define a tu audiencia',
        wizardPath: 'audience',
        managePath: 'audience',
        sectionKey: 'audience',
      },
    ],
  },
  {
    title: 'Crea tu contenido',
    items: [
      {
        label: 'Diseña tu temario',
        wizardPath: 'curriculum',
        managePath: 'curriculum',
        sectionKey: 'curriculum',
      },
    ],
  },
  {
    title: 'Publica tu curso',
    items: [
      {
        label: 'Página de inicio',
        wizardPath: 'basics',
        managePath: 'basics',
        sectionKey: 'basics',
      },
      {
        label: 'Precios',
        wizardPath: 'pricing',
        managePath: 'pricing',
        sectionKey: 'pricing',
      },
      {
        label: 'Promociones',
        wizardPath: 'promotions',
        managePath: 'promotions',
        sectionKey: 'promotions',
      },
      {
        label: 'Mensajes',
        wizardPath: 'messages',
        managePath: 'messages',
        sectionKey: 'messages',
      },
    ],
  },
];
