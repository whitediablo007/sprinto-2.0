import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';
import { TimerContainerComponent } from './features/time-tracking/containers/timer-container.component';
import * as AuthActions from './features/auth/store/auth.actions';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TimerContainerComponent],
  template: `
    <router-outlet></router-outlet>
    <app-timer-container></app-timer-container>
  `,
  styles: []
})
export class AppComponent implements OnInit {
  private store = inject(Store);
  title = 'Team Task Manager';

  ngOnInit(): void {
    // Загружаем сохранённые данные авторизации при старте приложения
    this.store.dispatch(AuthActions.loadUserFromStorage());
  }
}

