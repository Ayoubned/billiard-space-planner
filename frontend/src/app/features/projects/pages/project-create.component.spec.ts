import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Project } from '../../../core/models/project.model';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectCreateComponent } from './project-create.component';

describe('ProjectCreateComponent', () => {
  let fixture: ReturnType<
    typeof TestBed.createComponent<ProjectCreateComponent>
  >;
  let createProjectMock: ReturnType<typeof vi.fn>;
  let navigateMock: ReturnType<typeof vi.fn>;

  const mockProject: Project = {
    id: 'aa3644c2-3c9d-4e9e-ac4a-9f724cdd6ab9',
    name: 'New Billiard Room',
    clientName: 'Demo Client',
    siteAddress: 'Lyon, France',
    description: 'Test project',
    status: 'DRAFT',
    createdAt: '2026-11-15T19:00:14.336071Z',
    updatedAt: '2026-11-15T19:00:14.336071Z',
    archivedAt: null,
  };

  beforeEach(async () => {
    createProjectMock = vi.fn().mockReturnValue(of(mockProject));
    navigateMock = vi.fn();

    await TestBed.configureTestingModule({
      imports: [ProjectCreateComponent],
      providers: [
        {
          provide: ProjectService,
          useValue: {
            createProject: createProjectMock,
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: navigateMock,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectCreateComponent);
  });

  it('should create the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should reject an empty project name', () => {
    const component = fixture.componentInstance;

    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.name.touched).toBe(true);
    expect(createProjectMock).not.toHaveBeenCalled();
  });

  it('should reject a project name longer than 120 characters', () => {
    const component = fixture.componentInstance;

    component.form.controls.name.setValue('A'.repeat(121));
    component.submit();

    expect(component.form.controls.name.hasError('maxlength')).toBe(true);
    expect(createProjectMock).not.toHaveBeenCalled();
  });

  it('should trim values and omit empty optional fields', () => {
    const component = fixture.componentInstance;

    component.form.setValue({
      name: '  New Billiard Room  ',
      clientName: '  ',
      siteAddress: '  Lyon, France  ',
      description: '',
    });

    component.submit();

    expect(createProjectMock).toHaveBeenCalledWith({
      name: 'New Billiard Room',
      clientName: undefined,
      siteAddress: 'Lyon, France',
      description: undefined,
    });
  });

  it('should navigate to the projects list after successful creation', () => {
    const component = fixture.componentInstance;

    component.form.controls.name.setValue('New Billiard Room');
    component.submit();

    expect(navigateMock).toHaveBeenCalledWith(['/projects'], {
      queryParams: { created: '1' },
    });
  });

  it('should display a validation error when the backend returns 400', () => {
    createProjectMock.mockReturnValue(
      throwError(() => ({ status: 400 })),
    );

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('New Billiard Room');

    component.submit();

    expect(component.errorMessage()).toBe(
      'Some information is invalid. Please check the form.',
    );
    expect(component.isSubmitting()).toBe(false);
  });

  it('should display a connection error when the server is unreachable', () => {
    createProjectMock.mockReturnValue(
      throwError(() => ({ status: 0 })),
    );

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('New Billiard Room');

    component.submit();

    expect(component.errorMessage()).toBe(
      'Unable to reach the server. Check your connection.',
    );
    expect(component.isSubmitting()).toBe(false);
  });

  it('should display a generic error for other server errors', () => {
    createProjectMock.mockReturnValue(
      throwError(() => ({ status: 500 })),
    );

    const component = fixture.componentInstance;
    component.form.controls.name.setValue('New Billiard Room');

    component.submit();

    expect(component.errorMessage()).toBe(
      'The project could not be created. Please try again.',
    );
    expect(component.isSubmitting()).toBe(false);
  });

  it('should navigate to projects when cancel is called', () => {
    fixture.componentInstance.cancel();

    expect(navigateMock).toHaveBeenCalledWith(['/projects']);
  });
});