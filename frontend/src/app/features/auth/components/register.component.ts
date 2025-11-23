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
import { PasswordValidator } from '../../../shared/validators/password.validator';
import { PasswordRequirementsComponent } from '../../../shared/components/password-requirements.component';
import { tap } from 'rxjs/operators';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    ButtonModule,
    CardModule,
    MessagesModule,
    PasswordModule,
    PasswordRequirementsComponent
  ],
  template: `
    <div class="flex justify-content-center align-items-center min-h-screen bg-gray-100">
      <p-card header="Register" styleClass="p-shadow-3 w-full max-w-md">
        <div class="p-fluid">
          <p-messages *ngIf="errorMessage" [(value)]="messages" [enableService]="false" [closable]="true"></p-messages>

          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
            <div class="field">
              <label for="name">Name</label>
              <input id="name" type="text" pInputText formControlName="name" placeholder="Your full name" />
              <small
                *ngIf="
                  registerForm.get('name')?.invalid &&
                  registerForm.get('name')?.dirty
                "
                class="p-error"
              >
                <span *ngIf="registerForm.get('name')?.errors?.['required']"
                  >Name is required.</span
                >
                <span *ngIf="registerForm.get('name')?.errors?.['minlength']"
                  >Name must be at least 2 characters.</span
                >
              </small>
            </div>

            <div class="field">
              <label for="email">Email</label>
              <input id="email" type="email" pInputText formControlName="email" placeholder="your@email.com" />
              <small
                *ngIf="
                  registerForm.get('email')?.invalid &&
                  registerForm.get('email')?.dirty
                "
                class="p-error"
              >
                <span *ngIf="registerForm.get('email')?.errors?.['required']"
                  >Email is required.</span
                >
                <span *ngIf="registerForm.get('email')?.errors?.['email']"
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
                placeholder="Create a strong password"
              ></p-password>
              <small
                *ngIf="
                  registerForm.get('password')?.invalid &&
                  registerForm.get('password')?.dirty
                "
                class="p-error"
              >
                <span *ngIf="registerForm.get('password')?.errors?.['required']"
                  >Password is required.</span
                >
                <app-password-requirements
                  *ngIf="registerForm.get('password')?.errors?.['passwordStrength']"
                  [requirements]="registerForm.get('password')?.errors?.['passwordStrength']"
                ></app-password-requirements>
              </small>
            </div>

            <p-button
              label="Register"
              icon="pi pi-user-plus"
              type="submit"
              [loading]="loading$ | async"
              [disabled]="registerForm.invalid"
              styleClass="w-full mt-3"
            ></p-button>
          </form>

          <div class="mt-3 text-center">
            <a routerLink="/auth/login" class="p-link">Already have an account? Login</a>
          </div>
        </div>
      </p-card>
    </div>
  `,
  styles: []
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private store = inject(Store);
  private router = inject(Router);

  registerForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, PasswordValidator.strong()]],
  });

  loading$ = this.store.select(selectAuthLoading);
  messages: Message[] = [];
  errorMessage: string | null = null;

  constructor() {
    this.store.select(selectAuthError).pipe(
      tap(error => {
        if (error) {
          this.errorMessage = (error as any)?.error?.message || (typeof error === 'string' ? error : 'Registration failed. Please try again.');
          this.messages = [{ severity: 'error', summary: 'Error', detail: this.errorMessage || '' }];
        }
      })
    ).subscribe();
  }

  onSubmit() {
    if (this.registerForm.valid) {
      this.messages = [];
      this.errorMessage = null;
      const { name, email, password } = this.registerForm.value;
      this.store.dispatch(AuthActions.register({
        userData: { name: name!, email: email!, password: password! }
      }));
    }
  }
}

