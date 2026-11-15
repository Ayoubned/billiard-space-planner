import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'projects',
  },
  {
    path: 'projects/new',
    loadComponent: () =>
      import('./features/projects/pages/project-create.component').then(
        (component) => component.ProjectCreateComponent,
      ),
  },
  {
  path: 'projects/:id/edit',
  loadComponent: () =>
    import('./features/projects/pages/project-edit.component').then(
      (component) => component.ProjectEditComponent,
    ),
},
  {
    path: 'projects/:id',
    loadComponent: () =>
        import('./features/projects/pages/project-details.component').then(
        (component) => component.ProjectDetailsComponent,
        ),
    },
    {
    path: 'projects',
    loadComponent: () =>
      import('./features/projects/pages/projects-list.component').then(
        (component) => component.ProjectsListComponent,
      ),
  },
  {
    path: '**',
    redirectTo: 'projects',
  },
];