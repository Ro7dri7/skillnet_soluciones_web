import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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
export class ManageCurriculumComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  readonly courseId = signal<number | null>(null);

  ngOnInit(): void {
    const id = this.route.parent?.snapshot.paramMap.get('id');
    if (id && !Number.isNaN(Number(id))) {
      this.courseId.set(Number(id));
    }
  }
}
