import { ActionReducerMap } from '@ngrx/store';
import { authReducer, AuthState } from '../features/auth/store/auth.reducer';

/**
 * Root application state interface.
 * 
 * Combines all feature states into a single application state tree.
 * Each feature module should register its state slice here.
 */
export interface AppState {
  auth: AuthState;
  // Add other feature states here as they are implemented:
  // projects: ProjectsState;
  // tasks: TasksState;
  // notifications: NotificationsState;
  // etc.
}

/**
 * Root reducer map.
 * 
 * Maps each state slice to its corresponding reducer function.
 * Used by StoreModule.forRoot() in app configuration.
 */
export const rootReducers: ActionReducerMap<AppState> = {
  auth: authReducer
  // Add other feature reducers here
};

