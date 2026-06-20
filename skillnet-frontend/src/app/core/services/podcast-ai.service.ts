import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PodcastGenerateRequest {
  topic?: string;
  text?: string;
  courseId?: number;
  lessonId?: number;
  transcriptOnly?: boolean;
  language?: string;
  durationMinutes?: number;
}

export interface PodcastJobResponse {
  jobId: number;
  status: string;
  transcript?: string;
  audioUrl?: string | null;
  transcriptOnly?: boolean;
  language?: string;
  durationMinutes?: number;
  errorMessage?: string;
}

@Injectable({ providedIn: 'root' })
export class PodcastAiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/producer/podcast`;

  generate(payload: PodcastGenerateRequest): Observable<PodcastJobResponse> {
    return this.http.post<PodcastJobResponse>(`${this.base}/generate`, payload);
  }

  jobStatus(jobId: number): Observable<PodcastJobResponse> {
    return this.http.get<PodcastJobResponse>(`${this.base}/jobs/${jobId}`);
  }

  attach(jobId: number, lessonId: number): Observable<{ success: boolean; audioUrl?: string }> {
    return this.http.post<{ success: boolean; audioUrl?: string }>(
      `${this.base}/jobs/${jobId}/attach`,
      { lessonId },
    );
  }

  synthesizeAudio(jobId: number, approvedTranscript: string): Observable<PodcastJobResponse> {
    return this.http.post<PodcastJobResponse>(`${this.base}/jobs/${jobId}/synthesize`, {
      approvedTranscript,
    });
  }
}
