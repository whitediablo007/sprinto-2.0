import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';

/**
 * Presentational component for Forgot Password form (FR-007.1).
 *
 * Dumb component following Constitution Principle V:
 * - Uses @Input/@Output for communication
 * - No business logic or service dependencies
 * - OnPush change detection strategy
 */
@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="forgot-password-container">
      <div class="forgot-password-card">
        <div class="header">
          <h1>Восстановление пароля</h1>
          <p class="subtitle">
            Введите email, на который зарегистрирован аккаунт. Мы отправим вам ссылку для сброса
            пароля.
          </p>
        </div>

        <form [formGroup]="forgotPasswordForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="email">Email</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              placeholder="your@email.com"
              [class.invalid]="
                forgotPasswordForm.get('email')?.invalid &&
                forgotPasswordForm.get('email')?.touched
              "
            />
            <div
              *ngIf="
                forgotPasswordForm.get('email')?.invalid &&
                forgotPasswordForm.get('email')?.touched
              "
              class="error-message"
            >
              <span *ngIf="forgotPasswordForm.get('email')?.hasError('required')">
                Email обязателен
              </span>
              <span *ngIf="forgotPasswordForm.get('email')?.hasError('email')">
                Введите корректный email
              </span>
            </div>
          </div>

          <div *ngIf="successMessage" class="success-message">
            {{ successMessage }}
          </div>

          <div *ngIf="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>

          <button
            type="submit"
            class="btn-primary"
            [disabled]="forgotPasswordForm.invalid || isLoading"
          >
            <span *ngIf="!isLoading">Отправить ссылку</span>
            <span *ngIf="isLoading">Отправка...</span>
          </button>

          <div class="back-to-login">
            <a href="/auth/login" (click)="onBackToLogin($event)">← Вернуться к входу</a>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .forgot-password-container {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 20px;
      }

      .forgot-password-card {
        background: white;
        border-radius: 12px;
        padding: 40px;
        max-width: 450px;
        width: 100%;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
      }

      .header h1 {
        font-size: 28px;
        font-weight: 700;
        color: #1f2937;
        margin-bottom: 12px;
      }

      .subtitle {
        color: #6b7280;
        font-size: 14px;
        line-height: 1.5;
        margin-bottom: 30px;
      }

      .form-group {
        margin-bottom: 20px;
      }

      label {
        display: block;
        font-weight: 600;
        color: #374151;
        margin-bottom: 8px;
        font-size: 14px;
      }

      input {
        width: 100%;
        padding: 12px 16px;
        border: 2px solid #e5e7eb;
        border-radius: 8px;
        font-size: 14px;
        transition: all 0.2s;
      }

      input:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }

      input.invalid {
        border-color: #ef4444;
      }

      .error-message {
        color: #ef4444;
        font-size: 13px;
        margin-top: 6px;
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .success-message {
        background: #d1fae5;
        border: 1px solid #10b981;
        color: #065f46;
        padding: 12px;
        border-radius: 8px;
        font-size: 14px;
        margin-bottom: 20px;
      }

      .btn-primary {
        width: 100%;
        padding: 14px;
        background: #667eea;
        color: white;
        border: none;
        border-radius: 8px;
        font-size: 16px;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.2s;
        margin-top: 10px;
      }

      .btn-primary:hover:not(:disabled) {
        background: #5568d3;
      }

      .btn-primary:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .back-to-login {
        text-align: center;
        margin-top: 20px;
      }

      .back-to-login a {
        color: #667eea;
        text-decoration: none;
        font-size: 14px;
        font-weight: 500;
      }

      .back-to-login a:hover {
        text-decoration: underline;
      }
    `,
  ],
})
export class ForgotPasswordComponent {
  @Input() isLoading = false;
  @Input() successMessage: string | null = null;
  @Input() errorMessage: string | null = null;

  @Output() submitEmail = new EventEmitter<string>();
  @Output() backToLogin = new EventEmitter<void>();

  forgotPasswordForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  onSubmit(): void {
    if (this.forgotPasswordForm.valid) {
      this.submitEmail.emit(this.forgotPasswordForm.value.email);
    }
  }

  onBackToLogin(event: Event): void {
    event.preventDefault();
    this.backToLogin.emit();
  }
}

