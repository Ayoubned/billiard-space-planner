import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ProjectService } from '../../../core/services/project.service';

@Component({
  selector: 'app-project-edit',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './project-edit.component.html',
  styleUrl: './project-edit.component.scss',
})
export class ProjectEditComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly projectService = inject(ProjectService);

  readonly projectId = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    clientName: ['', Validators.maxLength(120)],
    siteAddress: ['', Validators.maxLength(250)],
    description: ['', Validators.maxLength(2000)],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.errorMessage.set('Invalid project ID.');
      this.isLoading.set(false);
      return;
    }

    this.projectId.set(id);
    this.loadProject(id);
  }

  private loadProject(id: string): void {
    this.projectService.getProject(id).subscribe({
      next: (project) => {
        this.form.patchValue({
          name: project.name,
          clientName: project.clientName ?? '',
          siteAddress: project.siteAddress ?? '',
          description: project.description ?? '',
        });

        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.status === 404
            ? 'This project does not exist.'
            : 'Unable to load the project.',
        );
        this.isLoading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.isSaving()) {
      this.form.markAllAsTouched();
      return;
    }

    const id = this.projectId();

    if (!id) {
      this.errorMessage.set('Invalid project ID.');
      return;
    }

    const values = this.form.getRawValue();

    this.isSaving.set(true);
    this.errorMessage.set(null);

    this.projectService.updateProject(id, {
      name: values.name.trim(),
      clientName: values.clientName.trim() || null,
      siteAddress: values.siteAddress.trim() || null,
      description: values.description.trim() || null,
    }).subscribe({
      next: () => {
        this.router.navigate(['/projects', id]);
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving.set(false);

        if (error.status === 400) {
          this.errorMessage.set(
            'Some information is invalid. Please check the form.',
          );
        } else if (error.status === 404) {
          this.errorMessage.set('This project no longer exists.');
        } else if (error.status === 0) {
          this.errorMessage.set(
            'Cannot reach the server. Check your connection.',
          );
        } else {
          this.errorMessage.set('Unable to save changes. Please try again.');
        }
      },
    });
  }

  cancel(): void {
    const id = this.projectId();

    if (id) {
      this.router.navigate(['/projects', id]);
    } else {
      this.router.navigate(['/projects']);
    }
  }
}