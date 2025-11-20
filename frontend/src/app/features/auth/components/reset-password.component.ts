import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PasswordValidator, PasswordRequirements } from '../../../shared/validators/password.validator';
import { PasswordRequirementsComponent } from '../../../shared/components/password-requirements.component';

/**
 * Presentational component for Reset Password form (FR-007.1, FR-007.2).
 *
 * Dumb component following Constitution Principle V:
 * - Uses @Input/@Output for communication
 * - No business logic or service dependencies
 * - OnPush change detection strategy
 * - Implements password validation per FR-007.2
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PasswordRequirementsComponent],
  template: `
    <div class="reset-password-container">
      <div class="reset-password-card">
        <div class="header">
          <h1>Создание нового пароля</h1>
          <p class="subtitle">Введите новый пароль для вашего аккаунта</p>
        </div>

        <form [formGroup]="resetPasswordForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="newPassword">Новый пароль</label>
            <input
              id="newPassword"
              type="password"
              formControlName="newPassword"
              placeholder="Минимум 8 символов"
              [class.invalid]="
                resetPasswordForm.get('newPassword')?.invalid &&
                resetPasswordForm.get('newPassword')?.touched
              "
            />
            <app-password-requirements 
              [requirements]="passwordRequirements" 
              [touched]="passwordTouched">
            </app-password-requirements>
          </div>

          <div class="form-group">
            <label for="confirmPassword">Подтвердите пароль</label>
            <input
              id="confirmPassword"
              type="password"
              formControlName="confirmPassword"
              placeholder="Повторите пароль"
              [class.invalid]="
                resetPasswordForm.get('confirmPassword')?.invalid &&
                resetPasswordForm.get('confirmPassword')?.touched
              "
            />
            <div
              *ngIf="
                resetPasswordForm.get('confirmPassword')?.invalid &&
                resetPasswordForm.get('confirmPassword')?.touched
              "
              class="error-message"
            >
              <span *ngIf="resetPasswordForm.hasError('passwordMismatch')">
                Пароли не совпадают
              </span>
            </div>
          </div>

          <div *ngIf="errorMessage" class="error-message-box">
            {{ errorMessage }}
          </div>

          <button
            type="submit"
            class="btn-primary"
            [disabled]="resetPasswordForm.invalid || isLoading"
          >
            <span *ngIf="!isLoading">Сохранить новый пароль</span>
            <span *ngIf="isLoading">Сохранение...</span>
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .reset-password-container {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 20px;
      }

      .reset-password-card {
        background: white;
        border-radius: 12px;
        padding: 40px;
        max-width: 500px;
        width: 100%;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
      }

      .header h1 {
        font-size: 28px;
        font-weight: 700;
        color: #1f2937;
        margin-bottom: 8px;
      }

      .subtitle {
        color: #6b7280;
        font-size: 14px;
        margin-bottom: 30px;
      }

      .form-group {
        margin-bottom: 24px;
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
      }

      .error-message-box {
        background: #fee2e2;
        border: 1px solid #ef4444;
        color: #991b1b;
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
    `,
  ],
})
export class ResetPasswordComponent implements OnInit {
  @Input() token: string | null = null;
  @Input() isLoading = false;
  @Input() errorMessage: string | null = null;

  @Output() submitPassword = new EventEmitter<{ token: string; newPassword: string }>();

  resetPasswordForm: FormGroup;
  passwordRequirements: PasswordRequirements | null = null;
  passwordTouched = false;

  constructor(private fb: FormBuilder) {
    this.resetPasswordForm = this.fb.group(
      {
        newPassword: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            Validators.maxLength(128),
            PasswordValidator.strong(),
          ],
        ],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: PasswordValidator.match('newPassword', 'confirmPassword') }
    );
  }

  ngOnInit(): void {
    this.resetPasswordForm.get('newPassword')?.valueChanges.subscribe((password) => {
      this.passwordTouched = true;
      this.passwordRequirements = PasswordValidator.checkRequirements(password || '');
    });
  }

  onSubmit(): void {
    if (this.resetPasswordForm.valid && this.token) {
      this.submitPassword.emit({
        token: this.token,
        newPassword: this.resetPasswordForm.value.newPassword,
      });
    }
  }
}

