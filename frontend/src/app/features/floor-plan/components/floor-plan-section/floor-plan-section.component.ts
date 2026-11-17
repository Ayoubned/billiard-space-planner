import { DatePipe } from '@angular/common';
import {
  Component,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { FloorPlanPreviewComponent } from '../floor-plan-preview/floor-plan-preview.component';
import { FloorPlan } from '../../models/floor-plan.model';
import { FloorPlanService } from '../../services/floor-plan.service';
import { FloorPlanUploadComponent } from '../floor-plan-upload/floor-plan-upload.component';

@Component({
  selector: 'app-floor-plan-section',
  standalone: true,
  imports: [DatePipe, FloorPlanPreviewComponent,FloorPlanUploadComponent],
  templateUrl: './floor-plan-section.component.html',
  styleUrl: './floor-plan-section.component.scss',
})
export class FloorPlanSectionComponent implements OnInit {
  private readonly floorPlanService = inject(FloorPlanService);

  readonly projectId = input.required<string>();
  readonly archived = input(false);

readonly downloadingId = signal<string | null>(null);
readonly deletingId = signal<string | null>(null);
readonly actionErrorMessage = signal<string | null>(null);

  readonly floorPlans = signal<FloorPlan[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadFloorPlans();
  }

  loadFloorPlans(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.floorPlanService.getFloorPlans(this.projectId()).subscribe({
      next: (floorPlans) => {
        this.floorPlans.set(floorPlans);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          'Unable to load floor plans. Please try again.',
        );
        this.isLoading.set(false);
      },
    });
  }

  onFloorPlanUploaded(floorPlan: FloorPlan): void {
    this.floorPlans.update((currentPlans) => [
      floorPlan,
      ...currentPlans.filter((plan) => plan.id !== floorPlan.id),
    ]);
  }

  floorPlanStatusLabel(status: FloorPlan['status']): string {
    const labels: Record<FloorPlan['status'], string> = {
      UPLOADED: 'Uploaded',
      PROCESSING: 'Processing',
      READY: 'Ready',
      FAILED: 'Failed',
    };

    return labels[status];
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  downloadFloorPlan(floorPlan: FloorPlan): void {
  if (this.downloadingId()) {
    return;
  }

  this.downloadingId.set(floorPlan.id);
  this.actionErrorMessage.set(null);

  this.floorPlanService
    .download(this.projectId(), floorPlan.id)
    .subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = url;
        link.download = floorPlan.originalFilename;
        link.style.display = 'none';

        document.body.appendChild(link);
        link.click();
        link.remove();

        // Give the browser time to start the download.
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);

        this.downloadingId.set(null);
      },
      error: () => {
        this.downloadingId.set(null);
        this.actionErrorMessage.set(
          `Unable to download "${floorPlan.originalFilename}". Please try again.`,
        );
      },
    });
}

deleteFloorPlan(floorPlan: FloorPlan): void {
  if (this.deletingId()) {
    return;
  }

  const confirmed = window.confirm(
    `Are you sure you want to delete "${floorPlan.originalFilename}"? This action cannot be undone.`,
  );

  if (!confirmed) {
    return;
  }

  this.deletingId.set(floorPlan.id);
  this.actionErrorMessage.set(null);

  this.floorPlanService
    .delete(this.projectId(), floorPlan.id)
    .subscribe({
      next: () => {
        this.floorPlans.update((plans) =>
          plans.filter((plan) => plan.id !== floorPlan.id),
        );

        this.deletingId.set(null);
      },
      error: () => {
        this.deletingId.set(null);
        this.actionErrorMessage.set(
          `Unable to delete "${floorPlan.originalFilename}". Please try again.`,
        );
      },
    });
}
}