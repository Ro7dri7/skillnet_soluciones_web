export interface BuilderStep {
  path: string;
  label: string;
}

export const BUILDER_STEPS: BuilderStep[] = [
  { path: 'type', label: 'Tipo de producto' },
  { path: 'title', label: 'Título' },
  { path: 'category', label: 'Categoría' },
  { path: 'subcategory', label: 'Subcategoría' },
  { path: 'audience', label: 'Audiencia' },
  { path: 'curriculum', label: 'Temario' },
];

export function builderStepIndex(path: string): number {
  const index = BUILDER_STEPS.findIndex((s) => s.path === path);
  return index >= 0 ? index : 0;
}

export function builderProgressPercent(path: string): number {
  const index = builderStepIndex(path);
  return Math.round(((index + 1) / BUILDER_STEPS.length) * 100);
}

export const COURSE_NEW_BASE = '/infoproductor/courses/new';

export function builderStepRoute(stepPath: string): string {
  return `${COURSE_NEW_BASE}/${stepPath}`;
}

export function extractBuilderStepFromUrl(url: string): string | null {
  const path = url.split('?')[0];
  if (path === COURSE_NEW_BASE || path === `${COURSE_NEW_BASE}/`) {
    return 'type';
  }
  const wizardMatch = path.match(/^\/infoproductor\/courses\/new\/([^/]+)$/);
  if (wizardMatch?.[1]) {
    return wizardMatch[1];
  }
  const legacyMatch = path.match(/^\/build\/([^/]+)$/);
  return legacyMatch?.[1] ?? null;
}

/** Pasos iniciales del wizard (sidebar principal, sin shell de personalización). */
export const WIZARD_ONLY_STEPS = new Set(['type', 'title', 'category', 'subcategory']);

/** Pasos del wizard sin sidebar principal (solo navbar + toolbar). */
export const WIZARD_STEPS_WITHOUT_MAIN_SIDEBAR = new Set(['title', 'category', 'subcategory']);

export function isWizardOnlyStep(path: string): boolean {
  const step = extractBuilderStepFromUrl(path);
  return step != null && WIZARD_ONLY_STEPS.has(step);
}

export function isWizardStepWithoutMainSidebar(path: string): boolean {
  const step = extractBuilderStepFromUrl(path);
  return step != null && WIZARD_STEPS_WITHOUT_MAIN_SIDEBAR.has(step);
}

export function builderPreviousRoute(path: string): string {
  const index = builderStepIndex(path);
  if (index <= 0) {
    return '/courses';
  }
  return builderStepRoute(BUILDER_STEPS[index - 1].path);
}

export function builderStepLabel(path: string): string {
  return BUILDER_STEPS.find((s) => s.path === path)?.label ?? '';
}
