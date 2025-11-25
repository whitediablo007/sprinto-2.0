import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessagesModule } from 'primeng/messages';
import { PasswordModule } from 'primeng/password';
import { Message } from 'primeng/api';
import * as AuthActions from '../store/auth.actions';
import { selectAuthError, selectAuthLoading } from '../store/auth.selectors';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    ButtonModule,
    CardModule,
    MessagesModule,
    PasswordModule
  ],
  template: `
    <div class="flex justify-content-center align-items-center min-h-screen bg-gray-100">
      <p-card header="Login" styleClass="p-shadow-3 w-full max-w-md">
        <div class="p-fluid">
          <p-messages *ngIf="errorMessage" [(value)]="messages" [enableService]="false" [closable]="true"></p-messages>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            <div class="field">
              <label for="email">Email</label>
              <input id="email" type="email" pInputText formControlName="email" placeholder="your@email.com" />
              <small
                *ngIf="
                  loginForm.get('email')?.invalid &&
                  loginForm.get('email')?.dirty
                "
                class="p-error"
              >
                <span *ngIf="loginForm.get('email')?.errors?.['required']"
                  >Email is required.</span
                >
                <span *ngIf="loginForm.get('email')?.errors?.['email']"
                  >Enter a valid email.</span
                >
              </small>
            </div>

            <div class="field">
              <label for="password">Password</label>
              <p-password
                id="password"
                formControlName="password"
                [toggleMask]="true"
                [feedback]="false"
                placeholder="Enter your password"
              ></p-password>
              <small
                *ngIf="
                  loginForm.get('password')?.invalid &&
                  loginForm.get('password')?.dirty
                "
                class="p-error"
              >
                Password is required.
              </small>
            </div>

            <p-button
              label="Login"
              icon="pi pi-sign-in"
              type="submit"
              [loading]="loading$ | async"
              [disabled]="loginForm.invalid"
              styleClass="w-full mt-3"
            ></p-button>
          </form>

          <div class="mt-3 text-center">
            <a routerLink="/auth/register" class="p-link">Don't have an account? Register</a>
          </div>
          <div class="mt-2 text-center">
            <a routerLink="/auth/forgot-password" class="p-link">Forgot Password?</a>
          </div>
        </div>
      </p-card>
    </div>
  `,
  styles: []
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private store = inject(Store);
  private router = inject(Router);

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  loading$ = this.store.select(selectAuthLoading);
  messages: Message[] = [];
  errorMessage: string | null = null;

  constructor() {
    this.store.select(selectAuthError).subscribe(error => {
      if (error) {
        this.errorMessage = (error as any)?.error?.message || (typeof error === 'string' ? error : 'Login failed. Please try again.');
        this.messages = [{ severity: 'error', summary: 'Error', detail: this.errorMessage || '' }];
      }
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.messages = [];
      this.errorMessage = null;
      const { email, password } = this.loginForm.value;
      this.store.dispatch(AuthActions.login({
        credentials: { email: email!, password: password! }
      }));
    }
  }
}
