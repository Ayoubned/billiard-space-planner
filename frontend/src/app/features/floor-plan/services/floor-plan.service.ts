import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  FloorPlan,
  FloorPlanInspection,
} from '../models/floor-plan.model';

import {
  HttpEvent,
  HttpEventType,
} from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class FloorPlanService {
  private readonly http = inject(HttpClient);

  private readonly projectsApiUrl = '/api/v1/projects';

  private floorPlansUrl(projectId: string): string {
    return `${this.projectsApiUrl}/${projectId}/floor-plans`;
  }

  upload(
  projectId: string,
  file: File,
): Observable<HttpEvent<FloorPlan>> {
  const formData = new FormData();
  formData.append('file', file, file.name);

  return this.http.post<FloorPlan>(
    this.floorPlansUrl(projectId),
    formData,
    {
      observe: 'events',
      reportProgress: true,
    },
  );
}

  getFloorPlans(projectId: string): Observable<FloorPlan[]> {
    return this.http.get<FloorPlan[]>(
      this.floorPlansUrl(projectId),
    );
  }

  getFloorPlan(
    projectId: string,
    floorPlanId: string,
  ): Observable<FloorPlan> {
    return this.http.get<FloorPlan>(
      `${this.floorPlansUrl(projectId)}/${floorPlanId}`,
    );
  }

  getInspection(
    projectId: string,
    floorPlanId: string,
  ): Observable<FloorPlanInspection> {
    return this.http.get<FloorPlanInspection>(
      `${this.floorPlansUrl(projectId)}/${floorPlanId}/inspection`,
    );
  }

  getPreview(
    projectId: string,
    floorPlanId: string,
  ): Observable<Blob> {
    return this.http.get(
      `${this.floorPlansUrl(projectId)}/${floorPlanId}/preview`,
      { responseType: 'blob' },
    );
  }

  download(
    projectId: string,
    floorPlanId: string,
  ): Observable<Blob> {
    return this.http.get(
      `${this.floorPlansUrl(projectId)}/${floorPlanId}/file`,
      { responseType: 'blob' },
    );
  }

  delete(
    projectId: string,
    floorPlanId: string,
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.floorPlansUrl(projectId)}/${floorPlanId}`,
    );
  }
}