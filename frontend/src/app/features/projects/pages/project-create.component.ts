import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';

import { ProjectService } from '../../../core/services/project.service';

@Component({
  selector: 'app-project-create',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './project-create.component.html',
  styleUrl: './project-create.component.scss',
})
export class ProjectCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly submitted = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    clientName: ['', Validators.maxLength(120)],
    siteAddress: ['', Validators.maxLength(250)],
    description: ['', Validators.maxLength(2000)],
  });

  cancel(): void {
    this.router.navigate(['/projects']);
  }

  submit(): void {
    this.submitted.set(true);
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue();

    this.isSubmitting.set(true);

    this.projectService
      .createProject({
        name: values.name.trim(),
        clientName: values.clientName.trim() || undefined,
        siteAddress: values.siteAddress.trim() || undefined,
        description: values.description.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.router.navigate(['/projects'], {
            queryParams: { created: '1' },
          });
        },
        error: (error) => {
          this.isSubmitting.set(false);

          if (error.status === 400) {
            this.errorMessage.set(
              'Some information is invalid. Please check the form.',
            );
          } else if (error.status === 0) {
            this.errorMessage.set(
              'Unable to reach the server. Check your connection.',
            );
          } else {
            this.errorMessage.set(
              'The project could not be created. Please try again.',
            );
          }
        },
      });
  }
}