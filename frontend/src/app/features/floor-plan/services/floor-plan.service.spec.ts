import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { FloorPlanService } from './floor-plan.service';
import {
  FloorPlan,
  FloorPlanInspection,
} from '../models/floor-plan.model';

import { HttpEventType } from '@angular/common/http';

describe('FloorPlanService', () => {
  let service: FloorPlanService;
  let httpMock: HttpTestingController;

  const projectId = 'project-uuid';
  const floorPlanId = 'floor-plan-uuid';

  const mockFloorPlan: FloorPlan = {
    id: floorPlanId,
    projectId,
    originalFilename: 'room-layout.pdf',
    contentType: 'application/pdf',
    fileSizeBytes: 1024,
    sha256: 'a'.repeat(64),
    status: 'READY',
    pageCount: 1,
    createdAt: '2026-11-17T10:00:00Z',
    updatedAt: '2026-11-17T10:00:00Z',
  };

  const mockInspection: FloorPlanInspection = {
    floorPlanId,
    type: 'PDF',
    pageCount: 1,
    pages: [
      {
        pageNumber: 1,
        widthPoints: 595,
        heightPoints: 842,
        rotationDegrees: 0,
      },
    ],
    image: null,
  };

  const baseUrl =
    `/api/v1/projects/${projectId}/floor-plans`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FloorPlanService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(FloorPlanService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  
  it('should upload a floor plan using multipart form data', () => {
    const file = new File(
      ['PDF content'],
      'room-layout.pdf',
      { type: 'application/pdf' },
    );

    service.upload(projectId, file).subscribe((event) => {
      if (event.type === HttpEventType.Response) {
        expect(event.body).toEqual(mockFloorPlan);
      }
    });

    const request = httpMock.expectOne(baseUrl);

    expect(request.request.method).toBe('POST');
    expect(request.request.body instanceof FormData).toBe(true);
    expect(request.request.body.get('file')).toBeInstanceOf(File);
    expect(request.request.headers.has('Content-Type')).toBe(false);
    expect(request.request.reportProgress).toBe(true);

    request.flush(mockFloorPlan);
  });


  it('should list floor plans for a project', () => {
    service.getFloorPlans(projectId).subscribe((response) => {
      expect(response).toEqual([mockFloorPlan]);
    });

    const request = httpMock.expectOne(baseUrl);

    expect(request.request.method).toBe('GET');

    request.flush([mockFloorPlan]);
  });

  it('should fetch a floor plan by ID', () => {
    service.getFloorPlan(projectId, floorPlanId).subscribe((response) => {
      expect(response).toEqual(mockFloorPlan);
    });

    const request = httpMock.expectOne(
      `${baseUrl}/${floorPlanId}`,
    );

    expect(request.request.method).toBe('GET');

    request.flush(mockFloorPlan);
  });

  it('should fetch inspection metadata', () => {
    service.getInspection(projectId, floorPlanId).subscribe((response) => {
      expect(response).toEqual(mockInspection);
    });

    const request = httpMock.expectOne(
      `${baseUrl}/${floorPlanId}/inspection`,
    );

    expect(request.request.method).toBe('GET');

    request.flush(mockInspection);
  });

  it('should fetch the preview as a Blob', () => {
    const blob = new Blob(['PDF preview'], {
      type: 'application/pdf',
    });

    service.getPreview(projectId, floorPlanId).subscribe((response) => {
      expect(response).toBeInstanceOf(Blob);
      expect(response.type).toBe('application/pdf');
    });

    const request = httpMock.expectOne(
      `${baseUrl}/${floorPlanId}/preview`,
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');

    request.flush(blob);
  });

  it('should download the original file as a Blob', () => {
    const blob = new Blob(['PDF download'], {
      type: 'application/pdf',
    });

    service.download(projectId, floorPlanId).subscribe((response) => {
      expect(response).toBeInstanceOf(Blob);
      expect(response.type).toBe('application/pdf');
    });

    const request = httpMock.expectOne(
      `${baseUrl}/${floorPlanId}/file`,
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');

    request.flush(blob);
  });

  it('should delete a floor plan', () => {
    service.delete(projectId, floorPlanId).subscribe((response) => {
      expect(response).toBeNull();
    });

    const request = httpMock.expectOne(
      `${baseUrl}/${floorPlanId}`,
    );

    expect(request.request.method).toBe('DELETE');

    request.flush(null);
  });
});