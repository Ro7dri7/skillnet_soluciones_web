import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import { courseLandingRouterLink } from '../../../../shared/utils/course-slug.util';

interface PublicCourseSummary {
  id: number;
  title: string;
  slug?: string;
  price?: number;
  coverImage?: string;
}

interface PublicInfoproductorProfile {
  id: number;
  username: string;
  firstName?: string;
  lastName?: string;
  bio?: string;
  professionalTitle?: string;
  yearsExperience?: number;
  company?: string;
  location?: string;
  website?: string;
  linkedinUrl?: string;
  profilePicture?: string;
  verifiedInfoproductor?: boolean;
  publishedCourses?: PublicCourseSummary[];
}

@Component({
  selector: 'app-public-infoproductor-profile',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './public-infoproductor-profile.component.html',
  styleUrl: './public-infoproductor-profile.component.scss',
})
export class PublicInfoproductorProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly profile = signal<PublicInfoproductorProfile | null>(null);

  ngOnInit(): void {
    const username = this.route.snapshot.paramMap.get('username');
    if (!username) {
      this.error.set('Perfil no encontrado.');
      this.loading.set(false);
      return;
    }
    this.http
      .get<PublicInfoproductorProfile>(
        `${environment.apiUrl}/public/infoproductors/${encodeURIComponent(username)}`,
      )
      .subscribe({
        next: (data) => {
          this.profile.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(messageFromHttpError(err, 'No se pudo cargar el perfil.'));
          this.loading.set(false);
        },
      });
  }

  displayName(p: PublicInfoproductorProfile): string {
    const full = [p.firstName, p.lastName].filter(Boolean).join(' ').trim();
    return full || p.username;
  }

  courseLink(course: PublicCourseSummary): (string | number)[] {
    if (course.slug) {
      return courseLandingRouterLink(course.slug);
    }
    return ['/marketplace/course', course.id];
  }
}
