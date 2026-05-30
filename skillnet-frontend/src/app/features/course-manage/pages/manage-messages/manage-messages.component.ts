import { Component, computed, effect, inject, OnDestroy, OnInit, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CourseManageContextService } from '../../../../core/services/course-manage-context.service';
import { CourseService } from '../../../../core/services/course.service';
import { ManageLayoutSaveService } from '../../../../core/services/manage-layout-save.service';
import { ProducerCoursesService } from '../../../../core/services/producer-courses.service';
import { ToastService } from '../../../../core/services/toast.service';

const MAX_MESSAGE_LENGTH = 1000;

@Component({
  selector: 'app-manage-messages',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './manage-messages.component.html',
  styleUrl: './manage-messages.component.scss',
})
export class ManageMessagesComponent implements OnInit, OnDestroy {
  private readonly manageContext = inject(CourseManageContextService);
  private readonly courseService = inject(CourseService);
  private readonly producerCourses = inject(ProducerCoursesService);
  private readonly manageSave = inject(ManageLayoutSaveService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(true);
  readonly welcomeMessage = signal('');
  readonly congratulationsMessage = signal('');
  readonly maxLength = MAX_MESSAGE_LENGTH;

  readonly welcomeLength = computed(() => this.welcomeMessage().length);
  readonly congratsLength = computed(() => this.congratulationsMessage().length);

  private loadedCourseId: number | null = null;

  constructor() {
    effect(() => {
      const id = this.manageContext.courseId();
      if (id != null && id !== this.loadedCourseId) {
        this.loadedCourseId = id;
        untracked(() => void this.loadCourse(id));
      }
    });
  }

  ngOnInit(): void {
    this.manageSave.registerSaveHandler(() => this.persistMessages());
  }

  ngOnDestroy(): void {
    this.manageSave.unregisterSaveHandler();
  }

  progressDotClass(length: number, threshold: number): string {
    return length > threshold ? 'bg-emerald-400' : 'bg-gray-200';
  }

  private async loadCourse(id: number): Promise<void> {
    try {
      const course = await firstValueFrom(this.courseService.getCourse(id));
      this.welcomeMessage.set(course.welcomeMessage ?? '');
      this.congratulationsMessage.set(course.congratulationsMessage ?? '');
    } catch {
      this.toast.error('No se pudieron cargar los mensajes.');
    } finally {
      this.loading.set(false);
    }
  }

  private async persistMessages(): Promise<void> {
    const courseId = this.manageContext.courseId();
    if (!courseId) {
      return;
    }
    await firstValueFrom(
      this.producerCourses.updateMessages(courseId, {
        welcomeMessage: this.welcomeMessage(),
        congratulationsMessage: this.congratulationsMessage(),
      }),
    );
  }
}
