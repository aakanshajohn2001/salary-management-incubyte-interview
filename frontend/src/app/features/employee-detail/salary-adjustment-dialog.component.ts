import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SalaryService } from '../../core/services/salary.service';
import { SalaryHistoryEntry } from '../../core/models/salary.model';
import { toIsoDate } from '../../core/utils/date.util';
import { extractErrorMessage } from '../../core/utils/http-error.util';

export interface SalaryAdjustmentDialogData {
  employeeId: number;
  currentCurrency: string | null;
}

@Component({
  selector: 'app-salary-adjustment-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './salary-adjustment-dialog.component.html',
  styleUrl: './salary-adjustment-dialog.component.scss',
})
export class SalaryAdjustmentDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly salaryService = inject(SalaryService);
  private readonly dialogRef = inject(MatDialogRef<SalaryAdjustmentDialogComponent, SalaryHistoryEntry | false>);
  readonly data = inject<SalaryAdjustmentDialogData>(MAT_DIALOG_DATA);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    effectiveDate: [null as Date | null, Validators.required],
    reason: ['', [Validators.required, Validators.maxLength(255)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();
    this.salaryService
      .addAdjustment(this.data.employeeId, {
        amount: raw.amount!,
        effectiveDate: toIsoDate(raw.effectiveDate!),
        reason: raw.reason,
      })
      .subscribe({
        next: (created) => {
          this.submitting.set(false);
          this.dialogRef.close(created);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.errorMessage.set(extractErrorMessage(error, 'Could not save this adjustment. Please try again.'));
        },
      });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
