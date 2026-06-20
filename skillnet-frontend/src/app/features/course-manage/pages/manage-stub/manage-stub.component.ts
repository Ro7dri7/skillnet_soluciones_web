import { Component, input } from '@angular/core';

@Component({
  selector: 'app-manage-stub',
  standalone: true,
  template: `
    <div class="mx-auto max-w-3xl p-8">
      <article class="rounded-xl border border-gray-200 bg-white p-10 text-center shadow-sm">
        <div
          class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-[#89ceff]/30 text-skillnet-dark"
        >
          <i class="ri-tools-line text-2xl"></i>
        </div>
        <h2 class="text-lg font-bold text-skillnet-dark">{{ title() }}</h2>
        <p class="mt-2 text-sm text-skillnet-muted">
          Esta sección se implementará en la siguiente fase del gestor de cursos.
        </p>
      </article>
    </div>
  `,
})
export class ManageStubComponent {
  readonly title = input.required<string>();
}
