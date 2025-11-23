import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { Store } from '@ngrx/store';
import { map } from 'rxjs/operators';
import { selectIsAuthenticated } from '../../features/auth/store/auth.selectors';

/**
 * Auth Guard for protecting routes that require authentication.
 * 
 * Redirects to login page if user is not authenticated.
 * Used with Angular Router's canActivate property.
 */
export const authGuard: CanActivateFn = () => {
  const store = inject(Store);
  const router = inject(Router);

  return store.select(selectIsAuthenticated).pipe(
    map(isAuthenticated => {
      if (isAuthenticated) {
        return true;
      }
      
      // Redirect to login page
      router.navigate(['/auth/login']);
      return false;
    })
  );
};

