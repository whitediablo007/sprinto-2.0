import { ActionReducerMap } from '@ngrx/store';
import { authReducer, AuthState } from '../features/auth/store/auth.reducer';
import { tasksReducer, TasksState } from '../features/tasks/store/tasks.reducer';
import { timeTrackingReducer, TimeTrackingState } from '../features/time-tracking/store/time-tracking.reducer';

/**
 * Root application state interface.
 * 
 * Combines all feature states into a single application state tree.
 * Each feature module should register its state slice here.
 */
export interface AppState {
  auth: AuthState;
  tasks: TasksState;
  timeTracking: TimeTrackingState;
  // Add other feature states here as they are implemented:
  // projects: ProjectsState;
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
  auth: authReducer,
  tasks: tasksReducer,
  timeTracking: timeTrackingReducer
  // Add other feature reducers here
};

