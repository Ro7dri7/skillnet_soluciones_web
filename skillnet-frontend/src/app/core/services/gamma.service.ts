import { Injectable, inject } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';



export type GammaFormat = 'document' | 'presentation';

export type GammaImageSource = 'aiGenerated' | 'noImages' | 'webFreeToUseCommercially';

export type GammaTextAmount = 'brief' | 'medium' | 'detailed' | 'extensive';



export interface GammaGenerateRequest {

  prompt: string;

  pages?: number;

  format?: GammaFormat;

  courseId?: number;

  lessonId?: number;

  title?: string;

  language?: string;

  tone?: string;

  audience?: string;

  textAmount?: GammaTextAmount;

  imageSource?: GammaImageSource;

  imageStyle?: string;

  additionalInstructions?: string;

  sourceMaterial?: string;

}



export interface GammaGenerationResponse {

  id?: string;

  status?: string;

  gammaUrl?: string;

  exportUrl?: string;

  platformExportUrl?: string;

  raw?: Record<string, unknown>;

}



export interface GammaReferenceExtractResponse {

  extractedText: string;

  characterCount: number;

}



@Injectable({ providedIn: 'root' })

export class GammaService {

  private readonly http = inject(HttpClient);

  private readonly base = `${environment.apiUrl}/producer/gamma`;



  start(payload: GammaGenerateRequest): Observable<GammaGenerationResponse> {

    return this.http.post<GammaGenerationResponse>(`${this.base}/generate`, payload);

  }



  status(generationId: string): Observable<GammaGenerationResponse> {

    return this.http.get<GammaGenerationResponse>(

      `${this.base}/status/${encodeURIComponent(generationId)}`,

    );

  }



  extractReferencePdf(file: File): Observable<GammaReferenceExtractResponse> {

    const form = new FormData();

    form.append('file', file);

    return this.http.post<GammaReferenceExtractResponse>(`${this.base}/extract-reference`, form);

  }

}


