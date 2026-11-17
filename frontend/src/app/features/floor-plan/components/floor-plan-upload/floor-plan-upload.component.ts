import {
  Component,
  input,
  output,
  signal,
} from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpEventType,
  HttpResponse,
} from '@angular/common/http';

import { FloorPlan } from '../../models/floor-plan.model';
import { FloorPlanService } from '../../services/floor-plan.service';

@Component({
  selector: 'app-floor-plan-upload',
  standalone: true,
  templateUrl: './floor-plan-upload.component.html',
  styleUrl: './floor-plan-upload.component.scss',
})
export class FloorPlanUploadComponent {
  readonly projectId = input.required<string>();
  readonly uploaded = output<FloorPlan>();

  readonly isDragging = signal(false);
  readonly isUploading = signal(false);
  readonly uploadProgress = signal<number | null>(null);
  readonly selectedFile = signal<File | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly maxFileSize = 25 * 1024 * 1024;

  private readonly allowedExtensions = [
    'pdf',
    'png',
    'jpg',
    'jpeg',
  ];

  constructor(
    private readonly floorPlanService: FloorPlanService,
  ) {}

  onFileSelected(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    const file = inputElement.files?.[0] ?? null;

    this.selectFile(file);

    // Allow selecting the same file again.
    inputElement.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();

    if (!this.isUploading()) {
      this.isDragging.set(true);
    }
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);

    if (this.isUploading()) {
      return;
    }

    const file = event.dataTransfer?.files?.[0] ?? null;
    this.selectFile(file);
  }

  selectFile(file: File | null): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.uploadProgress.set(null);

    if (!file) {
      return;
    }

    const validationError = this.validateFile(file);

    if (validationError) {
      this.selectedFile.set(null);
      this.errorMessage.set(validationError);
      return;
    }

    this.selectedFile.set(file);
  }

  uploadFile(): void {
    const file = this.selectedFile();

    if (!file || this.isUploading()) {
      return;
    }

    this.isUploading.set(true);
    this.uploadProgress.set(0);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.floorPlanService
      .upload(this.projectId(), file)
      .subscribe({
        next: (event: HttpEvent<FloorPlan>) => {
          if (event.type === HttpEventType.UploadProgress) {
            if (event.total) {
              const progress = Math.round(
                (event.loaded / event.total) * 100,
              );

              this.uploadProgress.set(
                Math.min(progress, 100),
              );
            }
          }

          if (event.type === HttpEventType.Response) {
            const response = event as HttpResponse<FloorPlan>;

            if (!response.body) {
              this.handleUploadError(
                'The server returned an empty response.',
              );
              return;
            }

            this.uploadProgress.set(100);
            this.isUploading.set(false);
            this.successMessage.set(
              'Floor plan uploaded successfully.',
            );
            this.selectedFile.set(null);

            this.uploaded.emit(response.body);
          }
        },
        error: (error: HttpErrorResponse) => {
          const message =
            error.status === 0
              ? 'Unable to reach the server. Check your connection.'
              : error.error?.message ||
                'Unable to upload the floor plan. Please try again.';

          this.handleUploadError(message);
        },
      });
  }

  clearSelectedFile(): void {
    if (this.isUploading()) {
      return;
    }

    this.selectedFile.set(null);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.uploadProgress.set(null);
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

  private validateFile(file: File): string | null {
    const extension = file.name
      .split('.')
      .pop()
      ?.toLowerCase();

    if (!extension || !this.allowedExtensions.includes(extension)) {
      return 'Unsupported file format. Choose a PDF, PNG, JPG or JPEG file.';
    }

    if (file.size === 0) {
      return 'The selected file is empty.';
    }

    if (file.size > this.maxFileSize) {
      return 'The file exceeds the maximum allowed size of 25 MB.';
    }

    const expectedMimeTypes: Record<string, string> = {
      pdf: 'application/pdf',
      png: 'image/png',
      jpg: 'image/jpeg',
      jpeg: 'image/jpeg',
    };

    const expectedMimeType = expectedMimeTypes[extension];

    if (file.type && file.type !== expectedMimeType) {
      return 'The file extension and MIME type do not match.';
    }

    return null;
  }

  private handleUploadError(message: string): void {
    this.isUploading.set(false);
    this.uploadProgress.set(null);
    this.errorMessage.set(message);
  }
}