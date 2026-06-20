import { DatePipe, DecimalPipe } from '@angular/common';
import {
  Component,
  computed,
  ElementRef,
  inject,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  ProducerQuizReviewService,
  QuizAnswerReview,
  QuizSubmissionReview,
} from '../../../../core/services/producer-quiz-review.service';
import {
  CourseProgressGroup,
  ProducerStudentsService,
  ProducerStudentProgressOverview,
  StudentProgressDetail,
} from '../../../../core/services/producer-students.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-producer-students',
  standalone: true,
  imports: [DecimalPipe, DatePipe, FormsModule, RouterLink],
  templateUrl: './producer-students.component.html',
  styleUrl: './producer-students.component.scss',
})
export class ProducerStudentsComponent implements OnInit {
  private readonly producerStudents = inject(ProducerStudentsService);
  private readonly quizReviewService = inject(ProducerQuizReviewService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly carouselRef = viewChild<ElementRef<HTMLElement>>('courseCarousel');

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly overview = signal<ProducerStudentProgressOverview | null>(null);
  readonly selectedCourseId = signal<number | null>(null);
  readonly searchTerm = signal('');
  readonly expandedStudentId = signal<number | null>(null);
  readonly studentsSectionOpen = signal(true);
  readonly quizReviewSectionOpen = signal(false);

  readonly quizLoading = signal(false);
  readonly quizRows = signal<QuizSubmissionReview[]>([]);
  readonly quizDetail = signal<QuizSubmissionReview | null>(null);
  readonly quizFeedback = signal('');
  readonly quizReviewing = signal(false);
  readonly gradingAnswerId = signal<number | null>(null);
  readonly quizModalOpen = signal(false);

  private initialCourseSlug: string | null = null;

  readonly courses = computed(() => this.overview()?.courses ?? []);

  readonly orderedCourses = computed(() => {
    const all = this.courses();
    const selectedId = this.selectedCourseId();
    if (!selectedId || all.length <= 1) {
      return all;
    }
    const selected = all.find((course) => course.id === selectedId);
    if (!selected) {
      return all;
    }
    return [selected, ...all.filter((course) => course.id !== selectedId)];
  });

  readonly selectedCourse = computed(() => {
    const id = this.selectedCourseId();
    return this.courses().find((c) => c.id === id) ?? this.courses()[0] ?? null;
  });

  readonly filteredStudents = computed(() => {
    const course = this.selectedCourse();
    if (!course) {
      return [] as StudentProgressDetail[];
    }
    const search = this.searchTerm().trim().toLowerCase();
    if (!search) {
      return course.students;
    }
    return course.students.filter((student) => {
      const haystack = `${student.userName} ${student.userEmail}`.toLowerCase();
      return haystack.includes(search);
    });
  });

  readonly filteredQuizRows = computed(() => {
    const courseId = this.selectedCourseId();
    const rows = this.quizRows();
    if (!courseId) {
      return rows;
    }
    return rows.filter((row) => row.courseId === courseId);
  });

  ngOnInit(): void {
    this.initialCourseSlug = this.normalizeSlug(
      this.route.snapshot.queryParamMap.get('course') ?? '',
    );
    const section = this.route.snapshot.queryParamMap.get('section');
    if (section === 'quiz-review' || this.router.url.includes('/quiz-review')) {
      this.quizReviewSectionOpen.set(true);
    }
    this.loadOverview();
    this.loadQuizList();

    const submissionId = this.route.snapshot.queryParamMap.get('submissionId');
    if (submissionId) {
      this.openQuizDetail(Number(submissionId));
    }
  }

  private loadOverview(): void {
    this.loading.set(true);
    this.error.set(null);

    this.producerStudents.overview().subscribe({
      next: (data) => {
        this.overview.set(data);
        this.selectedCourseId.set(this.resolveInitialCourseId(data));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo cargar el progreso de alumnos.'));
        this.loading.set(false);
      },
    });
  }

  private loadQuizList(): void {
    this.quizLoading.set(true);
    this.quizReviewService.list().subscribe({
      next: (data) => {
        this.quizRows.set(data);
        this.quizLoading.set(false);
      },
      error: () => {
        this.quizLoading.set(false);
      },
    });
  }

  private resolveInitialCourseId(data: ProducerStudentProgressOverview): number | null {
    if (this.initialCourseSlug) {
      const match = data.courses.find((course) => this.courseMatchesSlug(course, this.initialCourseSlug!));
      if (match) {
        return match.id;
      }
    }
    return data.courses[0]?.id ?? null;
  }

  private courseMatchesSlug(course: CourseProgressGroup, slug: string): boolean {
    const normalized = this.normalizeSlug(slug);
    const courseSlug = this.normalizeSlug(course.slug ?? '');
    return courseSlug === normalized || String(course.id) === normalized;
  }

  private normalizeSlug(value: string): string {
    if (!value) {
      return '';
    }
    try {
      return decodeURIComponent(value).trim().toLowerCase();
    } catch {
      return value.trim().toLowerCase();
    }
  }

  scrollCarousel(direction: 'left' | 'right'): void {
    const el = this.carouselRef()?.nativeElement;
    if (!el) {
      return;
    }
    el.scrollBy({ left: direction === 'left' ? -320 : 320, behavior: 'smooth' });
  }

  selectCourse(course: CourseProgressGroup): void {
    this.selectedCourseId.set(course.id);
    this.expandedStudentId.set(null);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { course: course.slug ?? course.id },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  toggleStudentsSection(): void {
    this.studentsSectionOpen.update((open) => !open);
  }

  toggleQuizReviewSection(): void {
    this.quizReviewSectionOpen.update((open) => !open);
  }

  toggleStudent(studentId: number): void {
    this.expandedStudentId.update((current) => (current === studentId ? null : studentId));
  }

  openQuizFromStudent(_student: StudentProgressDetail, quizSubmissionId?: number): void {
    if (!quizSubmissionId) {
      return;
    }
    this.openQuizDetail(quizSubmissionId);
  }

  openQuizDetail(id: number): void {
    this.quizModalOpen.set(true);
    this.quizReviewing.set(false);
    this.quizReviewService.get(id).subscribe({
      next: (data) => {
        this.quizDetail.set(data);
        this.quizFeedback.set(data.tutorFeedback ?? '');
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo cargar la entrega.'));
        this.closeQuizModal();
      },
    });
  }

  closeQuizModal(): void {
    this.quizModalOpen.set(false);
    this.quizDetail.set(null);
    this.quizFeedback.set('');
  }

  submitQuizReview(approved: boolean): void {
    const current = this.quizDetail();
    if (!current) {
      return;
    }
    this.quizReviewing.set(true);
    this.quizReviewService.review(current.id, { approved, feedback: this.quizFeedback() }).subscribe({
      next: (updated) => {
        this.quizDetail.set(updated);
        this.quizReviewing.set(false);
        this.loadQuizList();
        this.loadOverview();
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo registrar la revisión.'));
        this.quizReviewing.set(false);
      },
    });
  }

  gradeAnswer(answer: QuizAnswerReview, correct: boolean): void {
    const current = this.quizDetail();
    if (!current) {
      return;
    }
    this.gradingAnswerId.set(answer.id);
    this.quizReviewService
      .gradeAnswer(current.id, { answerId: answer.id, correct, feedback: answer.tutorFeedback })
      .subscribe({
        next: (result) => {
          this.quizDetail.update((prev) => {
            if (!prev) {
              return prev;
            }
            const answers = (prev.answers ?? []).map((item) =>
              item.id === answer.id ? { ...item, correct } : item,
            );
            return {
              ...prev,
              score: result.newScore,
              reviewStatus: result.reviewStatus ?? prev.reviewStatus,
              answers,
            };
          });
          this.gradingAnswerId.set(null);
        },
        error: (err) => {
          this.error.set(messageFromHttpError(err, 'No se pudo calificar la respuesta.'));
          this.gradingAnswerId.set(null);
        },
      });
  }

  isGrantable(answer: QuizAnswerReview): boolean {
    return answer.requiresManualGrading || answer.questionType === 'free_text';
  }

  courseImage(course: CourseProgressGroup): string {
    return course.imageUrl || 'assets/images/avatar-placeholder.png';
  }

  quizStatusLabel(status: string): string {
    switch (status) {
      case 'approved':
        return 'Aprobado';
      case 'rejected':
        return 'Rechazado';
      case 'pending':
        return 'Pendiente';
      case 'not_started':
        return 'Sin resolver';
      default:
        return status;
    }
  }
}
