import { Component, inject } from '@angular/core';
import { CourseManageContextService } from '../../../../core/services/course-manage-context.service';
import { CurriculumWorkspaceComponent } from '../../components/curriculum-workspace/curriculum-workspace.component';

@Component({
  selector: 'app-manage-curriculum',
  standalone: true,
  imports: [CurriculumWorkspaceComponent],
  template: `
    @if (courseId()) {
      <app-curriculum-workspace [initialCourseId]="courseId()!" />
    } @else {
      <app-curriculum-workspace />
    }
  `,
})
export class ManageCurriculumComponent {
  private readonly manageContext = inject(CourseManageContextService);

  readonly courseId = this.manageContext.courseId;
}
