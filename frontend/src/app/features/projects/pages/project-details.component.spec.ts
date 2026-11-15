import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { Project } from '../../../core/models/project.model';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectDetailsComponent } from './project-details.component';

describe('ProjectDetailsComponent', () => {
  let fixture: ComponentFixture<ProjectDetailsComponent>;
  let getProjectMock: ReturnType<typeof vi.fn>;
  let archiveProjectMock: ReturnType<typeof vi.fn>;
  let navigateMock: ReturnType<typeof vi.fn>;
  let confirmMock: ReturnType<typeof vi.spyOn>;

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

  beforeEach(async () => {
    getProjectMock = vi.fn().mockReturnValue(of(mockProject));
    archiveProjectMock = vi.fn().mockReturnValue(of(undefined));
    navigateMock = vi.fn();
    confirmMock = vi.spyOn(window, 'confirm').mockReturnValue(true);

    await TestBed.configureTestingModule({
      imports: [ProjectDetailsComponent],
      providers: [
        {
          provide: ProjectService,
          useValue: {
            getProject: getProjectMock,
            archiveProject: archiveProjectMock,
          },
        },
        {
          provide: Router,
          useValue: { navigate: navigateMock },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ id: projectId })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectDetailsComponent);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load and display project details', () => {
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(getProjectMock).toHaveBeenCalledWith(projectId);
    expect(compiled.textContent).toContain('Test Billiard Room');
    expect(compiled.textContent).toContain('Demo Client');
    expect(compiled.textContent).toContain('Lyon, France');
    expect(compiled.textContent).toContain('Draft');
  });

  it('should display an error when the project does not exist', () => {
    getProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Project unavailable');
    expect(compiled.textContent).toContain(
      'This project does not exist or has been deleted.',
    );
  });

  it('should display a generic error when loading fails', () => {
    getProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage()).toBe(
      'Unable to load project details. Please try again.',
    );
  });

  it('should show the invalid ID error when the route has no ID', async () => {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [ProjectDetailsComponent],
      providers: [
        {
          provide: ProjectService,
          useValue: {
            getProject: getProjectMock,
            archiveProject: archiveProjectMock,
          },
        },
        { provide: Router, useValue: { navigate: navigateMock } },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({})),
          },
        },
      ],
    }).compileComponents();

    const invalidFixture = TestBed.createComponent(ProjectDetailsComponent);
    invalidFixture.detectChanges();

    expect(invalidFixture.componentInstance.errorMessage()).toBe(
      'Invalid project ID.',
    );
    expect(getProjectMock).not.toHaveBeenCalled();
  });

  it('should not archive when confirmation is cancelled', () => {
    fixture.detectChanges();
    confirmMock.mockReturnValue(false);

    fixture.componentInstance.archiveProject();

    expect(confirmMock).toHaveBeenCalledOnce();
    expect(archiveProjectMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should archive the project and navigate to the projects list', () => {
    fixture.detectChanges();
    confirmMock.mockReturnValue(true);

    fixture.componentInstance.archiveProject();

    expect(confirmMock).toHaveBeenCalledOnce();
    expect(archiveProjectMock).toHaveBeenCalledWith(projectId);
    expect(navigateMock).toHaveBeenCalledWith(['/projects']);
  });

  it('should display an error when archiving fails', () => {
    archiveProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    fixture.detectChanges();
    fixture.componentInstance.archiveProject();

    expect(fixture.componentInstance.isArchiving()).toBe(false);
    expect(fixture.componentInstance.errorMessage()).toBe(
      'Unable to archive the project. Please try again.',
    );
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should display a specific error when the project no longer exists', () => {
    archiveProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    fixture.detectChanges();
    fixture.componentInstance.archiveProject();

    expect(fixture.componentInstance.errorMessage()).toBe(
      'This project no longer exists.',
    );
  });
});