import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { FloorPlan } from '../../models/floor-plan.model';
import { FloorPlanService } from '../../services/floor-plan.service';
import { FloorPlanSectionComponent } from './floor-plan-section.component';

describe('FloorPlanSectionComponent', () => {
  let fixture: ComponentFixture<FloorPlanSectionComponent>;
  let getFloorPlansMock: ReturnType<typeof vi.fn>;
  let downloadMock: ReturnType<typeof vi.fn>;
let deleteMock: ReturnType<typeof vi.fn>;
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
    getFloorPlansMock = vi.fn().mockReturnValue(of([]));
    downloadMock = vi.fn().mockReturnValue(
  of(new Blob(['file content'], { type: 'application/pdf' })),
);

deleteMock = vi.fn().mockReturnValue(of(undefined));
    await TestBed.configureTestingModule({
      imports: [FloorPlanSectionComponent],
      providers: [
        {
          provide: FloorPlanService,
          useValue: {
            getFloorPlans: getFloorPlansMock,
            upload: vi.fn().mockReturnValue(of(mockFloorPlan)),
            download: downloadMock,
            delete: deleteMock,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FloorPlanSectionComponent);
    fixture.componentRef.setInput('projectId', projectId);
  });

  it('should create the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load floor plans when initialized', () => {
    fixture.detectChanges();

    expect(getFloorPlansMock).toHaveBeenCalledWith(projectId);
    expect(fixture.componentInstance.floorPlans()).toEqual([]);

    expect(
      (fixture.nativeElement as HTMLElement).textContent,
    ).toContain('No floor plans uploaded yet.');
  });

  it('should display existing floor plans', () => {
    getFloorPlansMock.mockReturnValue(of([mockFloorPlan]));

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('room-layout.pdf');
    expect(compiled.textContent).toContain('Ready');
    expect(compiled.textContent).toContain('1 file');
  });

  it('should add an uploaded floor plan to the list', () => {
    fixture.detectChanges();

    fixture.componentInstance.onFloorPlanUploaded(mockFloorPlan);
    fixture.detectChanges();

    expect(fixture.componentInstance.floorPlans()).toContainEqual(
      mockFloorPlan,
    );

    expect(
      (fixture.nativeElement as HTMLElement).textContent,
    ).toContain('room-layout.pdf');
  });

  it('should display an error when floor plans cannot be loaded', () => {
    getFloorPlansMock.mockReturnValue(
      throwError(() => new Error('Network error')),
    );

    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage()).toBe(
      'Unable to load floor plans. Please try again.',
    );
  });

  it('should hide the upload component for archived projects', () => {
    fixture.componentRef.setInput('archived', true);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('app-floor-plan-upload')).toBeNull();
    expect(compiled.textContent).toContain(
      'This project is archived.',
    );
  });

  it('should refresh the floor-plan list when requested', () => {
    fixture.detectChanges();

    fixture.componentInstance.loadFloorPlans();

    expect(getFloorPlansMock).toHaveBeenCalledTimes(2);
  });


it('should delete a floor plan after confirmation', () => {
  vi.spyOn(window, 'confirm').mockReturnValue(true);

  fixture.detectChanges();
  fixture.componentInstance.floorPlans.set([mockFloorPlan]);

  fixture.componentInstance.deleteFloorPlan(mockFloorPlan);

  expect(deleteMock).toHaveBeenCalledWith(
    projectId,
    mockFloorPlan.id,
  );

  expect(fixture.componentInstance.floorPlans()).toEqual([]);
  expect(fixture.componentInstance.deletingId()).toBeNull();
});

it('should not delete a floor plan when confirmation is cancelled', () => {
  vi.spyOn(window, 'confirm').mockReturnValue(false);

  fixture.detectChanges();
  fixture.componentInstance.floorPlans.set([mockFloorPlan]);

  fixture.componentInstance.deleteFloorPlan(mockFloorPlan);

  expect(deleteMock).not.toHaveBeenCalled();
  expect(fixture.componentInstance.floorPlans()).toEqual([
    mockFloorPlan,
  ]);
});

it('should preserve the floor plan and show an error when deletion fails', () => {
  vi.spyOn(window, 'confirm').mockReturnValue(true);

  deleteMock.mockReturnValue(
    throwError(() => new Error('Delete failed')),
  );

  fixture.detectChanges();
  fixture.componentInstance.floorPlans.set([mockFloorPlan]);

  fixture.componentInstance.deleteFloorPlan(mockFloorPlan);

  expect(fixture.componentInstance.floorPlans()).toEqual([
    mockFloorPlan,
  ]);

  expect(fixture.componentInstance.deletingId()).toBeNull();
  expect(fixture.componentInstance.actionErrorMessage()).toContain(
    'Unable to delete',
  );
});

it('should download a floor plan using its original filename', () => {
  const urlMock = {
    createObjectURL: vi.fn().mockReturnValue('blob:test-download'),
    revokeObjectURL: vi.fn(),
  };
  vi.useFakeTimers();

  vi.stubGlobal('URL', urlMock);

  const clickSpy = vi
    .spyOn(HTMLAnchorElement.prototype, 'click')
    .mockImplementation(() => {});

  fixture.detectChanges();

  fixture.componentInstance.downloadFloorPlan(mockFloorPlan);

  expect(downloadMock).toHaveBeenCalledWith(
    projectId,
    mockFloorPlan.id,
  );

  expect(urlMock.createObjectURL).toHaveBeenCalled();
  expect(clickSpy).toHaveBeenCalled();
  vi.advanceTimersByTime(1000);
  expect(fixture.componentInstance.downloadingId()).toBeNull();
});

it('should display an error when downloading fails', () => {
  downloadMock.mockReturnValue(
    throwError(() => new Error('Download failed')),
  );

  fixture.detectChanges();

  fixture.componentInstance.downloadFloorPlan(mockFloorPlan);

  expect(fixture.componentInstance.downloadingId()).toBeNull();
  expect(fixture.componentInstance.actionErrorMessage()).toContain(
    'Unable to download',
  );
});

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

});