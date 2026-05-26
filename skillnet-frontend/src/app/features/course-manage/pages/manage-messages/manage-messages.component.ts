import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
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
  private readonly route = inject(ActivatedRoute);
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

  private courseId: number | null = null;

  ngOnInit(): void {
    const idParam = this.route.parent?.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (Number.isNaN(id)) {
      this.loading.set(false);
      return;
    }
    this.courseId = id;
    this.manageSave.registerSaveHandler(() => this.persistMessages());
    void this.loadCourse(id);
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
    if (!this.courseId) {
      return;
    }
    await firstValueFrom(
      this.producerCourses.updateMessages(this.courseId, {
        welcomeMessage: this.welcomeMessage(),
        congratulationsMessage: this.congratulationsMessage(),
      }),
    );
  }
}
