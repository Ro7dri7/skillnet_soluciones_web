import { Component, computed, effect, inject, signal, untracked } from '@angular/core';

import { Router, RouterLink } from '@angular/router';

import { firstValueFrom } from 'rxjs';

import { CourseManageContextService } from '../../../../core/services/course-manage-context.service';

import { CourseService } from '../../../../core/services/course.service';

import { ManageCurriculumService } from '../../../../core/services/manage-curriculum.service';

import { ManageLayoutSaveService } from '../../../../core/services/manage-layout-save.service';

import { ProducerCoursesService } from '../../../../core/services/producer-courses.service';

import { ProfileService } from '../../../../core/services/profile.service';

import { ToastService } from '../../../../core/services/toast.service';

import type { CourseResponse } from '../../../../shared/models/course.model';

import { getPublishBlockers, type PublishBlocker } from '../../../../shared/utils/publish-blockers.util';

import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

import { MANAGE_NAV_SECTIONS } from '../../data/manage-nav.data';



interface ChecklistItem {

  label: string;

  path: string;

  sectionKey: string;

  complete: boolean;

}



@Component({

  selector: 'app-manage-setup',

  standalone: true,

  imports: [RouterLink],

  templateUrl: './manage-setup.component.html',

  styleUrl: './manage-setup.component.scss',

})

export class ManageSetupComponent {

  private readonly manageContext = inject(CourseManageContextService);

  private readonly curriculum = inject(ManageCurriculumService);

  private readonly courseService = inject(CourseService);

  private readonly producerCourses = inject(ProducerCoursesService);

  private readonly profileService = inject(ProfileService);

  private readonly manageSave = inject(ManageLayoutSaveService);

  private readonly toast = inject(ToastService);

  private readonly router = inject(Router);



  readonly course = signal<CourseResponse | null>(null);

  readonly blockers = signal<PublishBlocker[]>([]);

  readonly statusUpdating = signal(false);

  readonly deleting = signal(false);



  readonly manageNavLink = (section: string) => this.manageContext.manageNavLink(section);



  readonly checklist = computed<ChecklistItem[]>(() => {

    const status = this.manageContext.sectionStatus();

    const items = MANAGE_NAV_SECTIONS.flatMap((section) => section.items);

    return items.map((item) => ({

      label: item.label,

      path: item.path,

      sectionKey: item.sectionKey,

      complete: status[item.sectionKey] ?? false,

    }));

  });



  readonly completedCount = computed(() => this.checklist().filter((i) => i.complete).length);

  readonly totalCount = computed(() => this.checklist().length);

  readonly readyToPublish = computed(() => this.blockers().length === 0);

  readonly progressPercent = computed(() =>

    this.totalCount() === 0 ? 0 : Math.round((this.completedCount() / this.totalCount()) * 100),

  );



  readonly isPublished = computed(() => this.course()?.status === 'published');

  readonly productNoun = computed(() => {

    const fmt = (this.course()?.courseFormat ?? '').toLowerCase();

    if (fmt === 'ebook') return 'ebook';

    if (fmt === 'podcast') return 'podcast';

    return 'curso';

  });



  constructor() {

    effect(() => {

      const id = this.manageContext.courseId();

      if (id != null) {

        untracked(() => void this.loadCourse(id));

      }

    });

  }



  private async loadCourse(id: number): Promise<void> {

    try {

      const course = await firstValueFrom(this.courseService.getCourse(id));

      this.course.set(course);

      await this.refreshBlockers(course);

    } catch (err) {

      this.toast.error(messageFromHttpError(err, 'No se pudo cargar el producto.'));

    }

  }



  private async refreshBlockers(course: CourseResponse): Promise<void> {

    let profile: { bio?: string | null; profilePicture?: string | null } | null = null;

    try {

      const me = await firstValueFrom(this.profileService.getMe());

      profile = {

        bio: (me as { bio?: string }).bio ?? null,

        profilePicture: me.profilePicture ?? null,

      };

    } catch {

      profile = null;

    }

    const blockers = getPublishBlockers(profile, course, this.manageContext.manageBasePath(), {

      curriculumComplete: this.manageContext.sectionStatus()['curriculum'],

    });

    this.blockers.set(blockers);

  }



  async togglePublish(): Promise<void> {

    const course = this.course();

    const id = this.manageContext.courseId();

    if (!course || id == null) {

      return;

    }



    const nextPublished = course.status !== 'published';

    if (nextPublished && this.blockers().length > 0) {

      window.alert(

        `No puedes publicar el ${this.productNoun()} hasta completar todos los requisitos pendientes.`,

      );

      return;

    }



    if (this.manageSave.showSaveButton()) {

      const saved = await this.manageSave.triggerSave();

      if (!saved) {

        return;

      }

    }



    this.statusUpdating.set(true);

    try {

      const response = await firstValueFrom(

        nextPublished

          ? this.producerCourses.publishCourse(id)

          : this.producerCourses.unpublishCourse(id),

      );

      this.course.update((c) => (c ? { ...c, status: response.status } : c));

      this.toast.success(nextPublished ? 'Producto publicado' : 'Producto en borrador');

    } catch (err) {

      this.toast.error(messageFromHttpError(err, 'No se pudo cambiar el estado.'));

    } finally {

      this.statusUpdating.set(false);

    }

  }



  async deleteProduct(): Promise<void> {

    const id = this.manageContext.courseId();

    if (id == null) {

      return;

    }

    if (

      !window.confirm(

        `¿Estás SEGURO de que quieres eliminar este ${this.productNoun()}? Esta acción no se puede deshacer.`,

      )

    ) {

      return;

    }

    this.deleting.set(true);

    try {

      await firstValueFrom(this.courseService.deleteCourse(id));

      this.toast.success('Producto eliminado');

      void this.router.navigate(['/courses']);

    } catch (err) {

      this.toast.error(messageFromHttpError(err, 'No se pudo eliminar el producto.'));

    } finally {

      this.deleting.set(false);

    }

  }

}

