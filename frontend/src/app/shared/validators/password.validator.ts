import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Password validation utilities per FR-007.2 requirements.
 *
 * Password must contain:
 * - Minimum 8 characters
 * - At least one digit
 * - At least one lowercase letter
 * - At least one uppercase letter
 * - At least one special character (@#$%^&+=!)
 */

export interface PasswordRequirements {
  hasMinLength: boolean;
  hasUpperCase: boolean;
  hasLowerCase: boolean;
  hasNumber: boolean;
  hasSpecialChar: boolean;
}

export class PasswordValidator {
  /**
   * Validate password strength per FR-007.2.
   *
   * @returns ValidatorFn for Angular Reactive Forms
   */
  static strong(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const password = control.value;

      if (!password) {
        return null; // Let required validator handle empty values
      }

      const requirements = PasswordValidator.checkRequirements(password);
      const allMet = Object.values(requirements).every((req) => req === true);

      if (!allMet) {
        return {
          passwordStrength: {
            requirements,
            message: 'Password does not meet security requirements',
          },
        };
      }

      return null;
    };
  }

  /**
   * Check password requirements.
   *
   * @param password password to check
   * @returns object with requirement flags
   */
  static checkRequirements(password: string): PasswordRequirements {
    return {
      hasMinLength: password.length >= 8,
      hasUpperCase: /[A-Z]/.test(password),
      hasLowerCase: /[a-z]/.test(password),
      hasNumber: /[0-9]/.test(password),
      hasSpecialChar: /[@#$%^&+=!]/.test(password),
    };
  }

  /**
   * Get user-friendly error messages for password validation.
   *
   * @param requirements password requirements status
   * @returns array of error messages for unmet requirements
   */
  static getErrorMessages(requirements: PasswordRequirements): string[] {
    const messages: string[] = [];

    if (!requirements.hasMinLength) {
      messages.push('Минимум 8 символов');
    }
    if (!requirements.hasUpperCase) {
      messages.push('Хотя бы одна заглавная буква');
    }
    if (!requirements.hasLowerCase) {
      messages.push('Хотя бы одна строчная буква');
    }
    if (!requirements.hasNumber) {
      messages.push('Хотя бы одна цифра');
    }
    if (!requirements.hasSpecialChar) {
      messages.push('Хотя бы один спецсимвол (@#$%^&+=!)');
    }

    return messages;
  }

  /**
   * Validator to ensure passwords match.
   *
   * @param passwordField name of password field
   * @param confirmPasswordField name of confirm password field
   * @returns ValidatorFn for form group
   */
  static match(passwordField: string, confirmPasswordField: string): ValidatorFn {
    return (formGroup: AbstractControl): ValidationErrors | null => {
      const password = formGroup.get(passwordField)?.value;
      const confirmPassword = formGroup.get(confirmPasswordField)?.value;

      if (!confirmPassword) {
        return null; // Let required validator handle empty values
      }

      if (password !== confirmPassword) {
        const confirmControl = formGroup.get(confirmPasswordField);
        confirmControl?.setErrors({ ...confirmControl.errors, passwordMismatch: true });
        return { passwordMismatch: true };
      }

      // Clear passwordMismatch error if passwords match
      const confirmControl = formGroup.get(confirmPasswordField);
      if (confirmControl?.hasError('passwordMismatch')) {
        const errors = { ...confirmControl.errors };
        delete errors['passwordMismatch'];
        confirmControl.setErrors(Object.keys(errors).length ? errors : null);
      }

      return null;
    };
  }
}

