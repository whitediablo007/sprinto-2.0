/**
 * E2E tests for password reset flow (FR-007.1).
 *
 * Tests the complete user journey:
 * 1. Request password reset
 * 2. Receive email (mocked)
 * 3. Click reset link
 * 4. Enter new password
 * 5. Login with new password
 */

describe('Password Reset Flow', () => {
  const testEmail = 'test@example.com';
  const oldPassword = 'OldPassword123!';
  const newPassword = 'NewPassword123!';

  beforeEach(() => {
    // Intercept API calls
    cy.intercept('POST', '/api/auth/password-reset/request').as('requestReset');
    cy.intercept('POST', '/api/auth/password-reset/confirm').as('confirmReset');
    cy.intercept('POST', '/api/auth/login').as('login');
  });

  it('should complete full password reset flow', () => {
    // Step 1: Navigate to forgot password page
    cy.visit('/auth/login');
    cy.contains('Забыли пароль?').click();
    cy.url().should('include', '/auth/forgot-password');

    // Step 2: Request password reset
    cy.get('input[type="email"]').type(testEmail);
    cy.contains('button', 'Отправить ссылку').click();

    // Wait for request to complete
    cy.wait('@requestReset').its('response.statusCode').should('eq', 200);

    // Should show success message
    cy.contains('Ссылка для восстановления отправлена').should('be.visible');

    // Step 3: Simulate clicking reset link (with token)
    const mockToken = 'mock-reset-token-123';
    cy.visit(`/auth/reset-password?token=${mockToken}`);

    // Step 4: Enter new password
    cy.get('input#newPassword').type(newPassword);
    cy.get('input#confirmPassword').type(newPassword);

    // Verify password requirements are met
    cy.contains('Минимум 8 символов').should('have.class', 'met');
    cy.contains('Одна заглавная буква').should('have.class', 'met');
    cy.contains('Одна строчная буква').should('have.class', 'met');
    cy.contains('Одна цифра').should('have.class', 'met');
    cy.contains('Спецсимвол').should('have.class', 'met');

    // Submit form
    cy.contains('button', 'Сохранить новый пароль').click();

    // Wait for confirmation
    cy.wait('@confirmReset').its('response.statusCode').should('eq', 200);

    // Should redirect to login
    cy.url().should('include', '/auth/login');

    // Step 5: Login with new password
    cy.get('input[type="email"]').type(testEmail);
    cy.get('input[type="password"]').type(newPassword);
    cy.contains('button', 'Войти').click();

    cy.wait('@login').its('response.statusCode').should('eq', 200);

    // Should be redirected to dashboard
    cy.url().should('include', '/dashboard');
  });

  it('should show validation errors for weak password', () => {
    const mockToken = 'mock-reset-token-123';
    cy.visit(`/auth/reset-password?token=${mockToken}`);

    // Test weak password
    const weakPassword = 'weak';
    cy.get('input#newPassword').type(weakPassword);

    // Should show unmet requirements
    cy.contains('Минимум 8 символов').should('have.class', 'unmet');
    cy.contains('Одна заглавная буква').should('have.class', 'unmet');
    cy.contains('Одна цифра').should('have.class', 'unmet');
    cy.contains('Спецсимвол').should('have.class', 'unmet');

    // Submit button should be disabled
    cy.contains('button', 'Сохранить новый пароль').should('be.disabled');
  });

  it('should validate password confirmation match', () => {
    const mockToken = 'mock-reset-token-123';
    cy.visit(`/auth/reset-password?token=${mockToken}`);

    cy.get('input#newPassword').type(newPassword);
    cy.get('input#confirmPassword').type('DifferentPassword123!');

    // Should show mismatch error
    cy.contains('Пароли не совпадают').should('be.visible');

    // Submit button should be disabled
    cy.contains('button', 'Сохранить новый пароль').should('be.disabled');
  });

  it('should handle expired token', () => {
    const expiredToken = 'expired-token-123';
    cy.visit(`/auth/reset-password?token=${expiredToken}`);

    cy.get('input#newPassword').type(newPassword);
    cy.get('input#confirmPassword').type(newPassword);
    cy.contains('button', 'Сохранить новый пароль').click();

    // Mock expired token response
    cy.wait('@confirmReset').its('response.statusCode').should('eq', 400);

    // Should show error message
    cy.contains('Срок действия ссылки истек').should('be.visible');
  });

  it('should allow returning to login page', () => {
    cy.visit('/auth/forgot-password');

    cy.contains('Вернуться к входу').click();
    cy.url().should('include', '/auth/login');
  });

  it('should not expose user existence (security)', () => {
    cy.visit('/auth/forgot-password');

    // Try with non-existing email
    cy.get('input[type="email"]').type('nonexisting@example.com');
    cy.contains('button', 'Отправить ссылку').click();

    cy.wait('@requestReset').its('response.statusCode').should('eq', 200);

    // Should still show success message (prevent user enumeration)
    cy.contains('Ссылка для восстановления отправлена').should('be.visible');
  });
});

/**
 * E2E tests for session inactivity (FR-007.3).
 */
describe('Session Inactivity', () => {
  const testEmail = 'test@example.com';
  const testPassword = 'TestPassword123!';

  beforeEach(() => {
    // Login before each test
    cy.intercept('POST', '/api/auth/login').as('login');
    cy.visit('/auth/login');
    cy.get('input[type="email"]').type(testEmail);
    cy.get('input[type="password"]').type(testPassword);
    cy.contains('button', 'Войти').click();
    cy.wait('@login');
  });

  it('should show warning dialog before session timeout', () => {
    // Mock idle timer to trigger warning faster (28 minutes of inactivity)
    cy.window().then((win) => {
      // Simulate time passing
      cy.clock(Date.now());
      cy.tick(28 * 60 * 1000); // 28 minutes
    });

    // Should show warning dialog
    cy.contains('Сессия скоро завершится').should('be.visible');
    cy.contains('Продолжить работу').should('be.visible');
    cy.contains('Выйти').should('be.visible');
  });

  it('should extend session when user clicks continue', () => {
    cy.window().then((win) => {
      cy.clock(Date.now());
      cy.tick(28 * 60 * 1000); // Trigger warning
    });

    cy.contains('Продолжить работу').click();

    // Warning should disappear
    cy.contains('Сессия скоро завершится').should('not.exist');

    // User should still be logged in
    cy.url().should('include', '/dashboard');
  });

  it('should logout on idle timeout', () => {
    cy.window().then((win) => {
      cy.clock(Date.now());
      cy.tick(30 * 60 * 1000); // 30 minutes - full timeout
    });

    // Should be redirected to login
    cy.url().should('include', '/auth/login');

    // Should show inactivity message
    cy.contains('Сессия завершена из-за неактивности').should('be.visible');
  });

  it('should reset timer on user activity', () => {
    cy.window().then((win) => {
      cy.clock(Date.now());
      cy.tick(20 * 60 * 1000); // 20 minutes idle
    });

    // Perform activity (click somewhere)
    cy.get('body').click();

    // Wait another 20 minutes (total would be 40 without reset)
    cy.window().then((win) => {
      cy.tick(20 * 60 * 1000);
    });

    // Should NOT be logged out (timer was reset)
    cy.url().should('include', '/dashboard');
  });
});

