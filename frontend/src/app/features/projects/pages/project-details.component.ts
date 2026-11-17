
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  Project,
  ProjectStatus,
} from '../../../core/models/project.model';
import { ProjectService } from '../../../core/services/project.service';
import { FloorPlanSectionComponent } from '../../floor-plan/components/floor-plan-section/floor-plan-section.component';
@Component({
  selector: 'app-project-details',
  standalone: true,
  imports: [DatePipe, RouterLink, FloorPlanSectionComponent],
  templateUrl: './project-details.component.html',
  styleUrl: './project-details.component.scss',
})
export class ProjectDetailsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly projectService = inject(ProjectService);

  readonly project = signal<Project | null>(null);
  readonly isLoading = signal(true);
  readonly isArchiving = signal(false);
  readonly errorMessage = signal<string | null>(null);


  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');

      if (!id) {
        this.errorMessage.set('Invalid project ID.');
        this.isLoading.set(false);
        return;
      }

      this.loadProject(id);
    });
  }

  private loadProject(id: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectService.getProject(id).subscribe({
      next: (project) => {
        this.project.set(project);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.status === 404
            ? 'This project does not exist or has been deleted.'
            : 'Unable to load project details. Please try again.',
        );
        this.isLoading.set(false);
      },
    });
  }


  archiveProject(): void {
    const currentProject = this.project();

    if (!currentProject || this.isArchiving()) {
      return;
    }

    const confirmed = window.confirm(
      `Are you sure you want to archive "${currentProject.name}"? You can still view archived projects later.`,
    );

    if (!confirmed) {
      return;
    }

    this.isArchiving.set(true);
    this.errorMessage.set(null);

    this.projectService.archiveProject(currentProject.id).subscribe({
      next: () => {
        this.router.navigate(['/projects']);
      },
      error: (error: HttpErrorResponse) => {
        this.isArchiving.set(false);

        this.errorMessage.set(
          error.status === 404
            ? 'This project no longer exists.'
            : 'Unable to archive the project. Please try again.',
        );
      },
    });
  }


  statusLabel(status: ProjectStatus): string {
    const labels: Record<ProjectStatus, string> = {
      DRAFT: 'Draft',
      FLOOR_PLAN_PENDING: 'Floor plan pending',
      FLOOR_PLAN_PROCESSING: 'Processing floor plan',
      GEOMETRY_VALIDATION: 'Geometry validation',
      EQUIPMENT_SELECTION: 'Equipment selection',
      OPTIMIZATION: 'Optimization',
      LAYOUT_REVIEW: 'Layout review',
      QUOTATION: 'Quotation',
      INSTALLATION_PLANNING: 'Installation planning',
      COMPLETED: 'Completed',
      ARCHIVED: 'Archived',
    };

    return labels[status];
  }


}
