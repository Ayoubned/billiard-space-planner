import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { Project } from '../../../core/models/project.model';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectEditComponent } from './project-edit.component';

describe('ProjectEditComponent', () => {
  let fixture: ComponentFixture<ProjectEditComponent>;
  let getProjectMock: ReturnType<typeof vi.fn>;
  let updateProjectMock: ReturnType<typeof vi.fn>;
  let navigateMock: ReturnType<typeof vi.fn>;

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
    updateProjectMock = vi.fn().mockReturnValue(of(mockProject));
    navigateMock = vi.fn();

    await TestBed.configureTestingModule({
      imports: [ProjectEditComponent],
      providers: [
        {
          provide: ProjectService,
          useValue: {
            getProject: getProjectMock,
            updateProject: updateProjectMock,
          },
        },
        {
          provide: Router,
          useValue: { navigate: navigateMock },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: projectId }),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectEditComponent);
  });

  it('should create the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load the existing project into the form', () => {
    fixture.detectChanges();

    const component = fixture.componentInstance;

    expect(getProjectMock).toHaveBeenCalledWith(projectId);
    expect(component.projectId()).toBe(projectId);
    expect(component.form.getRawValue()).toEqual({
      name: 'Test Billiard Room',
      clientName: 'Demo Client',
      siteAddress: 'Lyon, France',
      description: 'Test project',
    });
    expect(component.isLoading()).toBe(false);
  });

  it('should display an error when the project does not exist', () => {
    getProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage()).toBe(
      'This project does not exist.',
    );
    expect(fixture.componentInstance.isLoading()).toBe(false);
  });

  it('should display a generic error when loading fails', () => {
    getProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage()).toBe(
      'Unable to load the project.',
    );
  });

  it('should reject an empty project name', () => {
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('');

    component.submit();

    expect(component.form.controls.name.touched).toBe(true);
    expect(updateProjectMock).not.toHaveBeenCalled();
  });

  it('should update the project with trimmed values', () => {
    fixture.detectChanges();

    const component = fixture.componentInstance;

    component.form.setValue({
      name: '  Updated Billiard Room  ',
      clientName: '  Updated Client  ',
      siteAddress: '  Lyon, France  ',
      description: '  Updated description  ',
    });

    component.submit();

    expect(updateProjectMock).toHaveBeenCalledWith(projectId, {
      name: 'Updated Billiard Room',
      clientName: 'Updated Client',
      siteAddress: 'Lyon, France',
      description: 'Updated description',
    });
  });

  it('should send null for empty optional fields', () => {
    fixture.detectChanges();

    const component = fixture.componentInstance;

    component.form.setValue({
      name: 'Updated Billiard Room',
      clientName: '',
      siteAddress: '',
      description: '',
    });

    component.submit();

    expect(updateProjectMock).toHaveBeenCalledWith(projectId, {
      name: 'Updated Billiard Room',
      clientName: null,
      siteAddress: null,
      description: null,
    });
  });

  it('should navigate to project details after successful update', () => {
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('Updated Billiard Room');

    component.submit();

    expect(navigateMock).toHaveBeenCalledWith([
      '/projects',
      projectId,
    ]);
  });

  it('should display a validation error when the backend returns 400', () => {
    updateProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400 })),
    );

    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('Updated Billiard Room');

    component.submit();

    expect(component.errorMessage()).toBe(
      'Some information is invalid. Please check the form.',
    );
    expect(component.isSaving()).toBe(false);
  });

  it('should display a not-found error when the project was deleted', () => {
    updateProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('Updated Billiard Room');

    component.submit();

    expect(component.errorMessage()).toBe(
      'This project no longer exists.',
    );
  });

  it('should display a connection error when the server is unreachable', () => {
    updateProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('Updated Billiard Room');

    component.submit();

    expect(component.errorMessage()).toBe(
      'Cannot reach the server. Check your connection.',
    );
  });

  it('should display a generic error for other server failures', () => {
    updateProjectMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('Updated Billiard Room');

    component.submit();

    expect(component.errorMessage()).toBe(
      'Unable to save changes. Please try again.',
    );
  });

  it('should navigate to project details when cancel is called', () => {
    fixture.detectChanges();

    fixture.componentInstance.cancel();

    expect(navigateMock).toHaveBeenCalledWith([
      '/projects',
      projectId,
    ]);
  });
});