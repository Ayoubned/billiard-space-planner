import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Project, ProjectPage } from '../../../core/models/project.model';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectsListComponent } from './projects-list.component';

describe('ProjectsListComponent', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<ProjectsListComponent>>;
  let getProjectsMock: ReturnType<typeof vi.fn>;

  const mockProject: Project = {
    id: 'aa3644c2-3c9d-4e9e-ac4a-9f724cdd6ab9',
    name: 'Test Billiard Room',
    clientName: 'Demo Client',
    siteAddress: 'Lyon, France',
    description: 'Test project',
    status: 'DRAFT',
    createdAt: '2026-11-15T19:00:14.336071Z',
    updatedAt: '2026-11-15T19:00:14.336071Z',
    archivedAt: null,
  };

  const mockPage: ProjectPage = {
    projects: [mockProject],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 10,
  };

  function initializeComponent(): void {
    fixture.detectChanges();
  }

  beforeEach(async () => {
    getProjectsMock = vi.fn().mockReturnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [ProjectsListComponent],
      providers: [
        provideRouter([]),
        {
          provide: ProjectService,
          useValue: {
            getProjects: getProjectsMock,
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: of(convertToParamMap({ created: '1' })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectsListComponent);
  });

  it('should create the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load and display projects', () => {
    initializeComponent();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(getProjectsMock).toHaveBeenCalledOnce();
    expect(compiled.textContent).toContain('Test Billiard Room');
    expect(compiled.textContent).toContain('Demo Client');
    expect(compiled.textContent).toContain('Draft');
  });

  it('should display a success message after project creation', () => {
    initializeComponent();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.success-banner')).not.toBeNull();
    expect(compiled.textContent).toContain(
      'Project created successfully.',
    );
  });

  it('should display an empty state when no projects are found', () => {
    getProjectsMock.mockReturnValue(
      of({
        projects: [],
        totalElements: 0,
        totalPages: 0,
        page: 0,
        size: 10,
      }),
    );

    initializeComponent();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('No projects found');
  });

  it('should search projects using the entered search term', () => {
    initializeComponent();

    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector(
      'input[aria-label="Search projects"]',
    ) as HTMLInputElement;

    input.value = 'Billiard';
    input.dispatchEvent(new Event('input'));

    const searchButton = compiled.querySelector(
      '.search-box button',
    ) as HTMLButtonElement;

    searchButton.click();

    expect(getProjectsMock).toHaveBeenCalledTimes(2);
    expect(getProjectsMock).toHaveBeenLastCalledWith(
      expect.objectContaining({
        search: 'Billiard',
        page: 0,
        size: 10,
      }),
    );
  });

  it('should include archived projects when the archived status is selected', () => {
    initializeComponent();

    const compiled = fixture.nativeElement as HTMLElement;
    const select = compiled.querySelector(
      'select[aria-label="Filter by status"]',
    ) as HTMLSelectElement;

    select.value = 'ARCHIVED';
    select.dispatchEvent(new Event('change'));

    expect(getProjectsMock).toHaveBeenLastCalledWith(
      expect.objectContaining({
        status: 'ARCHIVED',
        includeArchived: true,
        page: 0,
      }),
    );
  });

  it('should display an error when the server is unreachable', () => {
    getProjectsMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    initializeComponent();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Something went wrong');
    expect(compiled.textContent).toContain(
      'Unable to reach the server.',
    );
  });

  it('should move to the next page', () => {
    getProjectsMock.mockReturnValue(
      of({
        ...mockPage,
        totalPages: 2,
      }),
    );

    initializeComponent();

    const compiled = fixture.nativeElement as HTMLElement;
    const nextButton = compiled.querySelector(
      '.pagination-actions button:last-child',
    ) as HTMLButtonElement;

    nextButton.click();

    expect(getProjectsMock).toHaveBeenLastCalledWith(
      expect.objectContaining({
        page: 1,
        size: 10,
      }),
    );
  });
});