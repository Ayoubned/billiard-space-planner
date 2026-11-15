import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { ProjectService } from './project.service';
import {
  CreateProjectRequest,
  Project,
  ProjectPage,
  UpdateProjectRequest,
} from '../models/project.model';

describe('ProjectService', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;

  const projectId = 'aa3644c2-3c9d-4e9e-ac4a-9f724cdd6ab9';

  const mockProject: Project = {
    id: projectId,
    name: 'Test Billiard Room',
    clientName: 'Demo Client',
    siteAddress: 'Lyon, France',
    description: 'Test project',
    status: 'DRAFT',
    createdAt: '2026-11-15T19:00:14.336071Z',
    updatedAt: '2026-11-15T19:00:14.336071Z',
    archivedAt: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProjectService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch projects with pagination and filters', () => {
    const mockResponse: ProjectPage = {
      projects: [mockProject],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    };

    service.getProjects({
      search: 'Billiard',
      page: 0,
      size: 10,
      sortBy: 'UPDATED_AT',
      direction: 'DESC',
    }).subscribe((response) => {
      expect(response).toEqual(mockResponse);
    });

    const request = httpMock.expectOne(
      (req) => req.url === '/api/v1/projects',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('Billiard');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('sortBy')).toBe('UPDATED_AT');
    expect(request.request.params.get('direction')).toBe('DESC');

    request.flush(mockResponse);
  });

  it('should fetch a project by ID', () => {
    service.getProject(projectId).subscribe((response) => {
      expect(response).toEqual(mockProject);
    });

    const request = httpMock.expectOne(
      `/api/v1/projects/${projectId}`,
    );

    expect(request.request.method).toBe('GET');

    request.flush(mockProject);
  });

  it('should create a project', () => {
    const payload: CreateProjectRequest = {
      name: 'New Billiard Room',
      clientName: 'New Client',
      siteAddress: 'Lyon, France',
      description: 'New project',
    };

    service.createProject(payload).subscribe((response) => {
      expect(response).toEqual(mockProject);
    });

    const request = httpMock.expectOne('/api/v1/projects');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(mockProject);
  });

  it('should update a project', () => {
    const payload: UpdateProjectRequest = {
      name: 'Updated Billiard Room',
      clientName: 'Updated Client',
      siteAddress: 'Villeurbanne, France',
      description: 'Updated project',
    };

    service.updateProject(projectId, payload).subscribe((response) => {
      expect(response).toEqual(mockProject);
    });

    const request = httpMock.expectOne(
      `/api/v1/projects/${projectId}`,
    );

    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);

    request.flush(mockProject);
  });

  it('should archive a project', () => {
    service.archiveProject(projectId).subscribe((response) => {
      expect(response).toBeNull();
    });

    const request = httpMock.expectOne(
      `/api/v1/projects/${projectId}`,
    );

    expect(request.request.method).toBe('DELETE');

    request.flush(null);
  });

  it('should omit empty and undefined query parameters', () => {
    service.getProjects({
      search: '',
      page: 0,
      size: 10,
    }).subscribe();

    const request = httpMock.expectOne(
      (req) => req.url === '/api/v1/projects',
    );

    expect(request.request.params.has('search')).toBe(false);
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');

    request.flush({
      projects: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 10,
    });
  });
});