import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RouterLink } from '@angular/router';
import {
  Project,
  ProjectStatus,
  ProjectPage,
} from '../../../core/models/project.model';
import { ProjectService } from '../../../core/services/project.service';

@Component({
  selector: 'app-projects-list',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './projects-list.component.html',
  styleUrl: './projects-list.component.scss',
})
export class ProjectsListComponent implements OnInit {
  readonly projects = signal<Project[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly successMessage = signal<string | null>(null);
  
  readonly searchTerm = signal('');
  readonly selectedStatus = signal<ProjectStatus | ''>('');

  readonly page = signal(0);
  readonly pageSize = 10;
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly statuses: { value: ProjectStatus; label: string }[] = [
    { value: 'DRAFT', label: 'Draft' },
    { value: 'FLOOR_PLAN_PENDING', label: 'Floor plan pending' },
    { value: 'FLOOR_PLAN_PROCESSING', label: 'Processing floor plan' },
    { value: 'GEOMETRY_VALIDATION', label: 'Geometry validation' },
    { value: 'EQUIPMENT_SELECTION', label: 'Equipment selection' },
    { value: 'OPTIMIZATION', label: 'Optimization' },
    { value: 'LAYOUT_REVIEW', label: 'Layout review' },
    { value: 'QUOTATION', label: 'Quotation' },
    { value: 'INSTALLATION_PLANNING', label: 'Installation planning' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'ARCHIVED', label: 'Archived' },
  ];

  constructor(
    private readonly projectService: ProjectService,
    private readonly route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
    if (params.get('created') === '1') {
      this.successMessage.set('Project created successfully.');
    }
  });
    this.loadProjects();
  }

  loadProjects(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectService
      .getProjects({
        search: this.searchTerm().trim() || undefined,
        status: this.selectedStatus() || undefined,
        includeArchived: this.selectedStatus() === 'ARCHIVED',
        page: this.page(),
        size: this.pageSize,
        sortBy: 'UPDATED_AT',
        direction: 'DESC',
      })
      .subscribe({
        next: (result: ProjectPage) => {
          this.projects.set(result.projects);
          this.totalElements.set(result.totalElements);
          this.totalPages.set(result.totalPages);
          this.isLoading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(
            error.status === 0
              ? 'Unable to reach the server. Check that the backend is running.'
              : 'Unable to load projects. Please try again.',
          );
          this.isLoading.set(false);
        },
      });
  }

  onSearchInput(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  onStatusChange(event: Event): void {
    this.selectedStatus.set(
      (event.target as HTMLSelectElement).value as ProjectStatus | '',
    );
    this.page.set(0);
    this.loadProjects();
  }

  search(): void {
    this.page.set(0);
    this.loadProjects();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedStatus.set('');
    this.page.set(0);
    this.loadProjects();
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update((current) => current - 1);
      this.loadProjects();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update((current) => current + 1);
      this.loadProjects();
    }
  }

  statusLabel(status: ProjectStatus): string {
    return this.statuses.find((item) => item.value === status)?.label ?? status;
  }

  trackProject(_index: number, project: Project): string {
    return project.id;
  }
}