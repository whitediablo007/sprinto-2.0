import { Routes } from '@angular/router';
import { DashboardComponent } from './containers/dashboard.component';
import { authGuard } from '../../core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: DashboardComponent,
    canActivate: [authGuard]
  }
];

