import { Component, inject } from '@angular/core';
import { CourseManageContextService } from '../../../../core/services/course-manage-context.service';
import { CurriculumWorkspaceComponent } from '../../components/curriculum-workspace/curriculum-workspace.component';

@Component({
  selector: 'app-manage-curriculum',
  standalone: true,
  imports: [CurriculumWorkspaceComponent],
  template: `
    @if (courseId(); as id) {
      <app-curriculum-workspace [initialCourseId]="id" />
    } @else {
      <div class="flex min-h-[50vh] items-center justify-center p-8">
        <p class="text-sm text-gray-500">Cargando producto…</p>
      </div>
    }
  `,
})
export class ManageCurriculumComponent {
  private readonly manageContext = inject(CourseManageContextService);

  readonly courseId = this.manageContext.courseId;
}
