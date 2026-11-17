export type FloorPlanStatus =
  | 'UPLOADED'
  | 'PROCESSING'
  | 'READY'
  | 'FAILED';

export interface FloorPlan {
  id: string;
  projectId: string;
  originalFilename: string;
  contentType: string;
  fileSizeBytes: number;
  sha256: string;
  status: FloorPlanStatus;
  pageCount: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface FloorPlanPageInspection {
  pageNumber: number;
  widthPoints: number;
  heightPoints: number;
  rotationDegrees: number;
}

export interface FloorPlanImageInspection {
  format: 'PNG' | 'JPEG';
  widthPixels: number;
  heightPixels: number;
  colorModel: string;
  dpiX: number | null;
  dpiY: number | null;
  physicalWidthInches: number | null;
  physicalHeightInches: number | null;
}

export interface FloorPlanInspection {
  floorPlanId: string;
  type: 'PDF' | 'IMAGE';
  pageCount: number | null;
  pages: FloorPlanPageInspection[];
  image: FloorPlanImageInspection | null;
}