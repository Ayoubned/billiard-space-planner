
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { FloorPlan } from '../../models/floor-plan.model';
import { FloorPlanService } from '../../services/floor-plan.service';
import { FloorPlanPreviewComponent } from './floor-plan-preview.component';

describe('FloorPlanPreviewComponent', () => {
  let fixture: ComponentFixture<FloorPlanPreviewComponent>;
  let getPreviewMock: ReturnType<typeof vi.fn>;

  const projectId = 'project-uuid';

  const mockFloorPlan: FloorPlan = {
    id: 'floor-plan-uuid',
    projectId,
    originalFilename: 'room-layout.pdf',
    contentType: 'application/pdf',
    fileSizeBytes: 1024,
    sha256: 'a'.repeat(64),
    status: 'READY',
    pageCount: 1,
    createdAt: '2026-11-17T10:00:00Z',
    updatedAt: '2026-11-17T10:00:00Z',
  };

  beforeEach(async () => {
    getPreviewMock = vi.fn().mockReturnValue(
      of(new Blob(['preview content'], { type: 'application/pdf' })),
    );

    await TestBed.configureTestingModule({
      imports: [FloorPlanPreviewComponent],
      providers: [
        {
          provide: FloorPlanService,
          useValue: {
            getPreview: getPreviewMock,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FloorPlanPreviewComponent);
    fixture.componentRef.setInput('projectId', projectId);
    fixture.componentRef.setInput('floorPlan', mockFloorPlan);

    const urlMock = {
  createObjectURL: vi.fn().mockReturnValue('blob:floor-plan-preview'),
  revokeObjectURL: vi.fn(),
};

vi.stubGlobal('URL', urlMock as unknown as typeof URL);
  });

  afterEach(() => {
    fixture.destroy();
    vi.unstubAllGlobals();
  });

  it('should create the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should not request a preview before the user opens it', () => {
    fixture.detectChanges();

    expect(getPreviewMock).not.toHaveBeenCalled();
  });

  it('should request and display a PDF preview', () => {
    fixture.componentInstance.openPreview();
    fixture.detectChanges();

    expect(getPreviewMock).toHaveBeenCalledWith(
      projectId,
      mockFloorPlan.id,
    );

    expect(fixture.componentInstance.isOpen()).toBe(true);
    expect(fixture.componentInstance.isLoading()).toBe(false);
    expect(fixture.componentInstance.previewUrl()).toBe(
      'blob:floor-plan-preview',
    );

    expect(
      fixture.nativeElement.querySelector('iframe'),
    ).not.toBeNull();
  });

  it('should display an image preview for PNG files', () => {
    const imageFloorPlan: FloorPlan = {
      ...mockFloorPlan,
      originalFilename: 'room-layout.png',
      contentType: 'image/png',
    };

    fixture.componentRef.setInput('floorPlan', imageFloorPlan);
    fixture.componentInstance.openPreview();
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('img'),
    ).not.toBeNull();

    expect(
      fixture.nativeElement.querySelector('iframe'),
    ).toBeNull();
  });

  it('should display an error when preview loading fails', () => {
    getPreviewMock.mockReturnValue(
      throwError(() => new Error('Preview failed')),
    );

    fixture.componentInstance.openPreview();
    fixture.detectChanges();

    expect(fixture.componentInstance.isLoading()).toBe(false);
    expect(fixture.componentInstance.errorMessage()).toBe(
      'Unable to load the preview. Please try again.',
    );

    expect(
      fixture.nativeElement.textContent,
    ).toContain('Unable to load the preview.');
  });

  it('should close the preview and release its object URL', () => {
    fixture.componentInstance.openPreview();

    fixture.componentInstance.closePreview();
    fixture.detectChanges();

    expect(fixture.componentInstance.isOpen()).toBe(false);
    expect(fixture.componentInstance.previewUrl()).toBeNull();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith(
      'blob:floor-plan-preview',
    );
  });

  it('should release the object URL when the component is destroyed', () => {
    fixture.componentInstance.openPreview();

    fixture.destroy();

    expect(URL.revokeObjectURL).toHaveBeenCalledWith(
      'blob:floor-plan-preview',
    );
  });

  it('should not make duplicate requests while the preview is open', () => {
    fixture.componentInstance.openPreview();
    fixture.componentInstance.openPreview();

    expect(getPreviewMock).toHaveBeenCalledTimes(1);
  });
});
