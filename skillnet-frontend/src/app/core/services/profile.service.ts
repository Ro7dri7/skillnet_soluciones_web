import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ProfileUser {
  id: number;
  username: string;
  email: string;
  role?: string;
  activeRole?: string;
  student?: boolean;
  infoproductor?: boolean;
  superUser?: boolean;
  staff?: boolean;
  firstName?: string;
  lastName?: string;
  bio?: string;
  professionalTitle?: string;
  phone?: string;
  countryCode?: string;
  company?: string;
  location?: string;
  website?: string;
  linkedinUrl?: string;
  instagramUrl?: string;
  youtubeUrl?: string;
  twitterUrl?: string;
  postalCode?: string;
  address?: string;
  profilePicture?: string;
  emailVerified?: boolean;
}

export interface UpdateProfilePayload {
  firstName?: string;
  lastName?: string;
  bio?: string;
  professionalTitle?: string;
  phone?: string;
  countryCode?: string;
  company?: string;
  location?: string;
  website?: string;
  linkedinUrl?: string;
  instagramUrl?: string;
  youtubeUrl?: string;
  postalCode?: string;
  address?: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  getMe(): Observable<ProfileUser> {
    return this.http.get<ProfileUser>(`${this.baseUrl}/me`);
  }

  updateMe(payload: UpdateProfilePayload): Observable<ProfileUser> {
    return this.http.put<ProfileUser>(`${this.baseUrl}/me`, payload);
  }

  changePassword(payload: ChangePasswordPayload): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/me/change-password`, payload);
  }
}
