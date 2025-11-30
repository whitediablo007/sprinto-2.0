import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Task, TaskCreateRequest, TaskStatus, TaskPriority } from '../../../shared/models/task.model';

/**
 * Task Form Component (Presentational).
 *
 * Reactive форма для создания/редактирования задачи.
 * Presentational component - вся логика и данные передаются через @Input/@Output.
 *
 * Использует OnPush change detection для максимальной производительности.
 */
@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="taskForm" (ngSubmit)="onSubmit()" class="space-y-4">
      <!-- Title -->
      <div>
        <label for="title" class="block text-sm font-medium text-gray-700 mb-1">
          Название задачи <span class="text-red-500">*</span>
        </label>
        <input
          id="title"
          type="text"
          formControlName="title"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          [class.border-red-500]="isFieldInvalid('title')"
          placeholder="Введите название задачи"
          [attr.aria-required]="true"
          [attr.aria-invalid]="isFieldInvalid('title')" />
        <p *ngIf="isFieldInvalid('title')" class="mt-1 text-sm text-red-600">Название обязательно</p>
      </div>

      <!-- Description -->
      <div>
        <label for="description" class="block text-sm font-medium text-gray-700 mb-1"> Описание </label>
        <textarea
          id="description"
          formControlName="description"
          rows="4"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="Опишите задачу подробнее"></textarea>
      </div>

      <!-- Project ID (hidden if editing) -->
      <div *ngIf="!editMode">
        <label for="projectId" class="block text-sm font-medium text-gray-700 mb-1">
          Проект <span class="text-red-500">*</span>
        </label>
        <input
          id="projectId"
          type="text"
          formControlName="projectId"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          [class.border-red-500]="isFieldInvalid('projectId')"
          placeholder="ID проекта" />
        <p *ngIf="isFieldInvalid('projectId')" class="mt-1 text-sm text-red-600">Проект обязателен</p>
      </div>

      <!-- Parent Task ID (optional) -->
      <div>
        <label for="parentTaskId" class="block text-sm font-medium text-gray-700 mb-1">
          Родительская задача (для подзадач)
        </label>
        <input
          id="parentTaskId"
          type="text"
          formControlName="parentTaskId"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="ID родительской задачи (опционально)" />
      </div>

      <!-- Priority and Status -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label for="priority" class="block text-sm font-medium text-gray-700 mb-1">
            Приоритет <span class="text-red-500">*</span>
          </label>
          <select
            id="priority"
            formControlName="priority"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            [class.border-red-500]="isFieldInvalid('priority')">
            <option [value]="TaskPriority.LOW">Низкий</option>
            <option [value]="TaskPriority.MEDIUM">Средний</option>
            <option [value]="TaskPriority.HIGH">Высокий</option>
            <option [value]="TaskPriority.URGENT">Срочно</option>
          </select>
          <p *ngIf="isFieldInvalid('priority')" class="mt-1 text-sm text-red-600">Приоритет обязателен</p>
        </div>

        <div>
          <label for="status" class="block text-sm font-medium text-gray-700 mb-1"> Статус </label>
          <select
            id="status"
            formControlName="status"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option [value]="TaskStatus.NEW">Новая</option>
            <option [value]="TaskStatus.IN_PROGRESS">В работе</option>
            <option [value]="TaskStatus.TESTING">Тестирование</option>
            <option [value]="TaskStatus.BLOCKED">Заблокирована</option>
            <option [value]="TaskStatus.COMPLETED">Завершена</option>
          </select>
        </div>
      </div>

      <!-- Assignee ID -->
      <div>
        <label for="assigneeId" class="block text-sm font-medium text-gray-700 mb-1"> Исполнитель </label>
        <input
          id="assigneeId"
          type="text"
          formControlName="assigneeId"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="ID исполнителя (опционально)" />
        <p class="mt-1 text-xs text-gray-500">В будущем здесь будет dropdown с пользователями проекта</p>
      </div>

      <!-- Deadline and Estimated Hours -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label for="deadline" class="block text-sm font-medium text-gray-700 mb-1"> Дедлайн </label>
          <input
            id="deadline"
            type="datetime-local"
            formControlName="deadline"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>

        <div>
          <label for="estimatedHours" class="block text-sm font-medium text-gray-700 mb-1"> Оценка (часы) </label>
          <input
            id="estimatedHours"
            type="number"
            min="0"
            step="0.5"
            formControlName="estimatedHours"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="0" />
        </div>
      </div>

      <!-- Form Actions -->
      <div class="flex justify-end gap-3 pt-4">
        <button
          type="button"
          (click)="onCancel()"
          class="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2">
          Отмена
        </button>
        <button
          type="submit"
          [disabled]="taskForm.invalid || isSubmitting"
          class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2">
          {{ editMode ? 'Сохранить' : 'Создать' }}
        </button>
      </div>
    </form>
  `,
})
export class TaskFormComponent implements OnInit {
  private fb = inject(FormBuilder);

  @Input() editMode: boolean = false;
  @Input() task: Task | null = null;
  @Input() projectId: string | null = null;
  @Input() isSubmitting: boolean = false;

  @Output() formSubmit = new EventEmitter<TaskCreateRequest>();
  @Output() formCancel = new EventEmitter<void>();

  taskForm!: FormGroup;
  TaskStatus = TaskStatus;
  TaskPriority = TaskPriority;

  ngOnInit(): void {
    this.initForm();

    if (this.task && this.editMode) {
      this.populateForm(this.task);
    } else if (this.projectId) {
      this.taskForm.patchValue({ projectId: this.projectId });
    }
  }

  private initForm(): void {
    this.taskForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(500)]],
      description: [''],
      projectId: [this.projectId || '', Validators.required],
      parentTaskId: [''],
      assigneeId: [''],
      status: [TaskStatus.NEW],
      priority: [TaskPriority.MEDIUM, Validators.required],
      deadline: [''],
      estimatedHours: [null],
    });
  }

  private populateForm(task: Task): void {
    this.taskForm.patchValue({
      title: task.title,
      description: task.description || '',
      projectId: task.projectId,
      parentTaskId: task.parentTaskId || '',
      assigneeId: task.assigneeId || '',
      status: task.status,
      priority: task.priority,
      deadline: task.deadline ? this.formatDateForInput(task.deadline) : '',
      estimatedHours: task.estimatedHours || null,
    });
  }

  private formatDateForInput(isoString: string): string {
    // Convert ISO 8601 datetime to datetime-local format (YYYY-MM-DDTHH:mm)
    const date = new Date(isoString);
    const offset = date.getTimezoneOffset();
    const localDate = new Date(date.getTime() - offset * 60 * 1000);
    return localDate.toISOString().slice(0, 16);
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.taskForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  onSubmit(): void {
    if (this.taskForm.invalid) {
      this.taskForm.markAllAsTouched();
      return;
    }

    const formValue = this.taskForm.value;

    // Convert datetime-local to ISO 8601 if deadline is set
    if (formValue.deadline) {
      formValue.deadline = new Date(formValue.deadline).toISOString();
    }

    // Remove empty optional fields
    const taskData: TaskCreateRequest = {
      title: formValue.title,
      projectId: formValue.projectId,
      priority: formValue.priority,
      status: formValue.status || TaskStatus.NEW,
      ...(formValue.description && { description: formValue.description }),
      ...(formValue.parentTaskId && { parentTaskId: formValue.parentTaskId }),
      ...(formValue.assigneeId && { assigneeId: formValue.assigneeId }),
      ...(formValue.deadline && { deadline: formValue.deadline }),
      ...(formValue.estimatedHours != null && { estimatedHours: formValue.estimatedHours }),
    };

    this.formSubmit.emit(taskData);
  }

  onCancel(): void {
    this.formCancel.emit();
  }
}

