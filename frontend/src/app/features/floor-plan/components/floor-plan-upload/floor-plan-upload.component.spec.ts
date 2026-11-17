import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpEvent,
  HttpEventType,
  HttpResponse,
  HttpUploadProgressEvent,
} from '@angular/common/http';
import { Subject } from 'rxjs';

import { FloorPlan } from '../../models/floor-plan.model';
import { FloorPlanService } from '../../services/floor-plan.service';
import { FloorPlanUploadComponent } from './floor-plan-upload.component';

describe('FloorPlanUploadComponent', () => {
  let fixture: ComponentFixture<FloorPlanUploadComponent>;
  let uploadSubject: Subject<HttpEvent<FloorPlan>>;
  let uploadMock: ReturnType<typeof vi.fn>;

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

  const createFile = (
    name = 'room-layout.pdf',
    type = 'application/pdf',
    content = 'test content',
  ): File => new File([content], name, { type });

  beforeEach(async () => {
    uploadSubject = new Subject<HttpEvent<FloorPlan>>();
    uploadMock = vi.fn().mockReturnValue(uploadSubject.asObservable());

    await TestBed.configureTestingModule({
      imports: [FloorPlanUploadComponent],
      providers: [
        {
          provide: FloorPlanService,
          useValue: {
            upload: uploadMock,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FloorPlanUploadComponent);
    fixture.componentRef.setInput('projectId', projectId);
  });

  afterEach(() => {
    uploadSubject.complete();
  });

  it('should create the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should accept a valid PDF file', () => {
    const component = fixture.componentInstance;

    component.selectFile(createFile());

    expect(component.selectedFile()?.name).toBe('room-layout.pdf');
    expect(component.errorMessage()).toBeNull();
  });

  it('should reject unsupported file extensions', () => {
    const component = fixture.componentInstance;

    component.selectFile(
      createFile('room-layout.dwg', 'application/octet-stream'),
    );

    expect(component.selectedFile()).toBeNull();
    expect(component.errorMessage()).toContain(
      'Unsupported file format',
    );
  });

  it('should reject files larger than 25 MB', () => {
    const component = fixture.componentInstance;

    const largeFile = new File(
      [new Uint8Array(25 * 1024 * 1024 + 1)],
      'large-plan.pdf',
      { type: 'application/pdf' },
    );

    component.selectFile(largeFile);

    expect(component.selectedFile()).toBeNull();
    expect(component.errorMessage()).toContain(
      'maximum allowed size of 25 MB',
    );
  });

  it('should reject empty files', () => {
    const component = fixture.componentInstance;

    component.selectFile(
      new File([], 'empty.pdf', { type: 'application/pdf' }),
    );

    expect(component.selectedFile()).toBeNull();
    expect(component.errorMessage()).toContain('empty');
  });

  it('should reject a mismatched MIME type', () => {
    const component = fixture.componentInstance;

    component.selectFile(
      createFile('room-layout.pdf', 'image/png'),
    );

    expect(component.selectedFile()).toBeNull();
    expect(component.errorMessage()).toContain(
      'extension and MIME type do not match',
    );
  });

  it('should upload the selected file and emit the response', () => {
    const component = fixture.componentInstance;
    const file = createFile();
    const uploadedSpy = vi.fn();

    component.uploaded.subscribe(uploadedSpy);
    component.selectFile(file);
    component.uploadFile();

    expect(uploadMock).toHaveBeenCalledWith(projectId, file);
    expect(component.isUploading()).toBe(true);

    uploadSubject.next(
      new HttpResponse<FloorPlan>({
        body: mockFloorPlan,
        status: 201,
      }),
    );

    expect(component.isUploading()).toBe(false);
    expect(component.uploadProgress()).toBe(100);
    expect(component.successMessage()).toContain('successfully');
    expect(component.selectedFile()).toBeNull();
    expect(uploadedSpy).toHaveBeenCalledWith(mockFloorPlan);
  });

  it('should update upload progress', () => {
    const component = fixture.componentInstance;

    component.selectFile(createFile());
    component.uploadFile();

    const progressEvent: HttpUploadProgressEvent = {
      type: HttpEventType.UploadProgress,
      loaded: 50,
      total: 100,
    };

    uploadSubject.next(progressEvent);

    expect(component.uploadProgress()).toBe(50);
    expect(component.isUploading()).toBe(true);
  });

  it('should display an error when upload fails', () => {
    const component = fixture.componentInstance;

    component.selectFile(createFile());
    component.uploadFile();

    uploadSubject.error({
      status: 500,
      error: { message: 'Upload failed' },
    });

    expect(component.isUploading()).toBe(false);
    expect(component.uploadProgress()).toBeNull();
    expect(component.errorMessage()).toBe('Upload failed');
  });

  it('should format file sizes', () => {
    const component = fixture.componentInstance;

    expect(component.formatFileSize(500)).toBe('500 B');
    expect(component.formatFileSize(2048)).toBe('2.0 KB');
    expect(component.formatFileSize(1048576)).toBe('1.0 MB');
  });

  it('should clear the selected file', () => {
    const component = fixture.componentInstance;

    component.selectFile(createFile());
    component.clearSelectedFile();

    expect(component.selectedFile()).toBeNull();
    expect(component.errorMessage()).toBeNull();
  });
});