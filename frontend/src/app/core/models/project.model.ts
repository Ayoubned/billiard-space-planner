export type ProjectStatus =
  | 'DRAFT'
  | 'FLOOR_PLAN_PENDING'
  | 'FLOOR_PLAN_PROCESSING'
  | 'GEOMETRY_VALIDATION'
  | 'EQUIPMENT_SELECTION'
  | 'OPTIMIZATION'
  | 'LAYOUT_REVIEW'
  | 'QUOTATION'
  | 'INSTALLATION_PLANNING'
  | 'COMPLETED'
  | 'ARCHIVED';

export type ProjectSortField = 'NAME' | 'CREATED_AT' | 'UPDATED_AT';

export type SortDirection = 'ASC' | 'DESC';

export interface Project {
  id: string;
  name: string;
  clientName: string | null;
  siteAddress: string | null;
  description: string | null;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
}

export interface CreateProjectRequest {
  name: string;
  clientName?: string | null;
  siteAddress?: string | null;
  description?: string | null;
}

export type UpdateProjectRequest = CreateProjectRequest;

export interface ProjectListParams {
  search?: string;
  status?: ProjectStatus;
  includeArchived?: boolean;
  page?: number;
  size?: number;
  sortBy?: ProjectSortField;
  direction?: SortDirection;
}

export interface ProjectPage {
  projects: Project[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}