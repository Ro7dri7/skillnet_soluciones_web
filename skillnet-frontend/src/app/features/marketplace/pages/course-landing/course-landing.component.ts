import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../../core/services/course.service';
import { StudentService } from '../../../../core/services/student.service';
import { CourseResponse } from '../../../../shared/models/course.model';
import { courseCoverUrl } from '../../../../shared/utils/marketplace-course.util';
import {
  courseLearnPath,
  courseSlugLookupCandidates,
  normalizeCourseSlugForUrl,
} from '../../../../shared/utils/course-slug.util';

@Component({
  selector: 'app-course-landing',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './course-landing.component.html',
})
export class CourseLandingComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly studentService = inject(StudentService);

  readonly course = signal<CourseResponse | null>(null);
  readonly isLoading = signal(true);
  readonly notFound = signal(false);
  readonly isEnrolled = signal(false);

  readonly coverUrl = computed(() => courseCoverUrl(this.course()));

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.notFound.set(true);
      this.isLoading.set(false);
      return;
    }

    this.courseService.getCourseBySlug(slug).subscribe({
      next: (found) => {
        const canonical = normalizeCourseSlugForUrl(found.slug);
        if (!courseSlugLookupCandidates(slug).includes(found.slug) && slug !== canonical) {
          void this.router.navigate(['/marketplace/course', canonical], { replaceUrl: true });
          return;
        }
        this.course.set(found);
        this.notFound.set(false);
        this.isLoading.set(false);
        this.checkEnrollment(canonical);
      },
      error: () => {
        this.notFound.set(true);
        this.isLoading.set(false);
      },
    });
  }

  learnPath(): string {
    const slug = this.course()?.slug;
    return slug ? courseLearnPath(slug) : '/mis-cursos';
  }

  levelLabel(level: string): string {
    const map: Record<string, string> = {
      beginner: 'Principiante',
      intermediate: 'Intermedio',
      advanced: 'Avanzado',
    };
    return map[level] ?? level;
  }

  private checkEnrollment(slug: string): void {
    this.studentService.getMyCourses().subscribe({
      next: (courses) => {
        const enrolled = courses.some(
          (c) => normalizeCourseSlugForUrl(c.slug) === normalizeCourseSlugForUrl(slug),
        );
        this.isEnrolled.set(enrolled);
        if (enrolled) {
          void this.router.navigate(['/marketplace/course', normalizeCourseSlugForUrl(slug), 'learn'], {
            replaceUrl: true,
          });
        }
      },
      error: () => this.isEnrolled.set(false),
    });
  }
}
