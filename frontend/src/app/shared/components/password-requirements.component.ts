import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PasswordRequirements } from '../validators/password.validator';

/**
 * Shared presentational component for displaying password requirements (FR-007.2).
 *
 * Shows visual indicators for each password requirement:
 * - ✓ Green checkmark for met requirements
 * - ○ Gray circle for pending requirements
 * - Red for unmet requirements when touched
 */
@Component({
  selector: 'app-password-requirements',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="password-requirements" *ngIf="requirements">
      <div
        class="requirement"
        [class.met]="requirements.hasMinLength"
        [class.unmet]="!requirements.hasMinLength && touched"
      >
        <span class="icon">{{ requirements.hasMinLength ? '✓' : '○' }}</span>
        Минимум 8 символов
      </div>
      <div
        class="requirement"
        [class.met]="requirements.hasUpperCase"
        [class.unmet]="!requirements.hasUpperCase && touched"
      >
        <span class="icon">{{ requirements.hasUpperCase ? '✓' : '○' }}</span>
        Одна заглавная буква
      </div>
      <div
        class="requirement"
        [class.met]="requirements.hasLowerCase"
        [class.unmet]="!requirements.hasLowerCase && touched"
      >
        <span class="icon">{{ requirements.hasLowerCase ? '✓' : '○' }}</span>
        Одна строчная буква
      </div>
      <div
        class="requirement"
        [class.met]="requirements.hasNumber"
        [class.unmet]="!requirements.hasNumber && touched"
      >
        <span class="icon">{{ requirements.hasNumber ? '✓' : '○' }}</span>
        Одна цифра
      </div>
      <div
        class="requirement"
        [class.met]="requirements.hasSpecialChar"
        [class.unmet]="!requirements.hasSpecialChar && touched"
      >
        <span class="icon">{{ requirements.hasSpecialChar ? '✓' : '○' }}</span>
        Спецсимвол (&#64;#$%^&+=!)
      </div>
    </div>
  `,
  styles: [
    `
      .password-requirements {
        margin-top: 12px;
        padding: 12px;
        background: #f9fafb;
        border-radius: 6px;
      }

      .requirement {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        color: #6b7280;
        margin-bottom: 6px;
      }

      .requirement:last-child {
        margin-bottom: 0;
      }

      .requirement.met {
        color: #10b981;
      }

      .requirement.unmet {
        color: #ef4444;
      }

      .requirement .icon {
        font-weight: bold;
        font-size: 14px;
      }
    `,
  ],
})
export class PasswordRequirementsComponent {
  @Input() requirements: PasswordRequirements | null = null;
  @Input() touched = false;
}

