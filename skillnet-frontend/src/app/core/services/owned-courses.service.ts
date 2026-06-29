import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { StudentService } from './student.service';

@Injectable({ providedIn: 'root' })
export class OwnedCoursesService {
  private readonly authService = inject(AuthService);
  private readonly studentService = inject(StudentService);

  private readonly ownedIdsSignal = signal<Set<number>>(new Set());
  private loading = false;

  readonly ownedCourseIds = this.ownedIdsSignal.asReadonly();

  refresh(): void {
    if (!this.authService.isLoggedIn()) {
      this.ownedIdsSignal.set(new Set());
      return;
    }
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.studentService.getMyCourses().subscribe({
      next: (courses) => {
        this.ownedIdsSignal.set(new Set(courses.map((course) => course.courseId)));
        this.loading = false;
      },
      error: () => {
        this.ownedIdsSignal.set(new Set());
        this.loading = false;
      },
    });
  }

  isOwned(courseId: number): boolean {
    return this.ownedIdsSignal().has(courseId);
  }
}
