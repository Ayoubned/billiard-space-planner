
import { Component, inject, input, OnDestroy, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subscription } from 'rxjs';

import { FloorPlan } from '../../models/floor-plan.model';
import { FloorPlanService } from '../../services/floor-plan.service';

@Component({
  selector: 'app-floor-plan-preview',
  standalone: true,
  templateUrl: './floor-plan-preview.component.html',
  styleUrl: './floor-plan-preview.component.scss',
})
export class FloorPlanPreviewComponent implements OnDestroy {
  private readonly floorPlanService = inject(FloorPlanService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly projectId = input.required<string>();
  readonly floorPlan = input.required<FloorPlan>();

  readonly isOpen = signal(false);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly safePreviewUrl = signal<SafeResourceUrl | null>(null);

  private objectUrl: string | null = null;
  private previewRequest: Subscription | null = null;

  get isPdf(): boolean {
    return this.floorPlan().contentType === 'application/pdf';
  }

  openPreview(): void {
    if (this.isLoading() || this.isOpen()) {
      return;
    }

    this.isOpen.set(true);
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.previewRequest = this.floorPlanService
      .getPreview(this.projectId(), this.floorPlan().id)
      .subscribe({
        next: (blob) => {
          this.releaseObjectUrl();

          this.objectUrl = URL.createObjectURL(blob);
          this.previewUrl.set(this.objectUrl);
          this.safePreviewUrl.set(
            this.sanitizer.bypassSecurityTrustResourceUrl(
              this.objectUrl,
            ),
          );

          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.errorMessage.set(
            'Unable to load the preview. Please try again.',
          );
        },
      });
  }

  closePreview(): void {
    this.previewRequest?.unsubscribe();
    this.previewRequest = null;

    this.isOpen.set(false);
    this.isLoading.set(false);
    this.errorMessage.set(null);
    this.releaseObjectUrl();
  }

  ngOnDestroy(): void {
    this.previewRequest?.unsubscribe();
    this.releaseObjectUrl();
  }

  private releaseObjectUrl(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }

    this.previewUrl.set(null);
    this.safePreviewUrl.set(null);
  }
}
