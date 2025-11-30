import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import * as AuthActions from '../../auth/store/auth.actions';
import { selectAuthUser } from '../../auth/store/auth.selectors';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, CardModule, ButtonModule],
  template: `
    <div class="min-h-screen bg-gray-100 p-4">
      <div class="container mx-auto">
        <div class="flex justify-content-between align-items-center mb-4">
          <h1 class="text-3xl font-bold text-gray-800">Dashboard</h1>
          <p-button 
            label="Logout" 
            icon="pi pi-sign-out" 
            (onClick)="onLogout()"
            styleClass="p-button-danger"
          ></p-button>
        </div>

        <p-card header="Welcome to Team Task Manager" styleClass="mb-4">
          <div *ngIf="user$ | async as user">
            <p class="text-lg mb-2">Hello, <strong>{{ user.name }}</strong>!</p>
            <p class="text-gray-600">Email: {{ user.email }}</p>
          </div>
        </p-card>

        <div class="grid">
          <div class="col-12 md:col-6 lg:col-3">
            <p-card header="Projects" styleClass="text-center">
              <i class="pi pi-folder text-6xl text-primary mb-3"></i>
              <p class="text-2xl font-bold">0</p>
              <p class="text-gray-600">Active Projects</p>
            </p-card>
          </div>

          <div class="col-12 md:col-6 lg:col-3">
            <p-card header="Tasks" styleClass="text-center">
              <i class="pi pi-check-square text-6xl text-success mb-3"></i>
              <p class="text-2xl font-bold">0</p>
              <p class="text-gray-600">Total Tasks</p>
            </p-card>
          </div>

          <div class="col-12 md:col-6 lg:col-3">
            <p-card header="Team Members" styleClass="text-center">
              <i class="pi pi-users text-6xl text-info mb-3"></i>
              <p class="text-2xl font-bold">0</p>
              <p class="text-gray-600">Team Members</p>
            </p-card>
          </div>

          <div class="col-12 md:col-6 lg:col-3">
            <p-card header="Time Tracked" styleClass="text-center">
              <i class="pi pi-clock text-6xl text-warning mb-3"></i>
              <p class="text-2xl font-bold">0h</p>
              <p class="text-gray-600">This Week</p>
            </p-card>
          </div>
        </div>

        <p-card header="Getting Started" styleClass="mt-4">
          <p class="mb-3">This is your Team Task Manager dashboard. Here's what you can do:</p>
          <ul class="list-disc pl-5">
            <li class="mb-2">Create and manage projects</li>
            <li class="mb-2">Assign tasks to team members</li>
            <li class="mb-2">Track time spent on tasks</li>
            <li class="mb-2">View analytics and reports</li>
            <li class="mb-2">Collaborate with your team in real-time</li>
          </ul>
          <p class="mt-3 text-gray-600">
            <i class="pi pi-info-circle mr-2"></i>
            More features will be available as we implement them.
          </p>
        </p-card>
      </div>
    </div>
  `,
  styles: []
})
export class DashboardComponent {
  private store = inject(Store);
  private router = inject(Router);

  user$ = this.store.select(selectAuthUser);

  onLogout() {
    this.store.dispatch(AuthActions.logout());
  }
}








