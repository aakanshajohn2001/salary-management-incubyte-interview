import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Employee } from '../../core/models/employee.model';
import { SalaryHistoryEntry } from '../../core/models/salary.model';
import { EmployeeService } from '../../core/services/employee.service';
import { SalaryAdjustmentDialogComponent } from './salary-adjustment-dialog.component';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.scss',
})
export class EmployeeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly employeeService = inject(EmployeeService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['effectiveDate', 'amount', 'reason'];
  readonly employee = signal<Employee | null>(null);
  readonly history = signal<SalaryHistoryEntry[]>([]);
  readonly loading = signal(false);

  private employeeId!: number;

  ngOnInit(): void {
    this.employeeId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    forkJoin([
      this.employeeService.get(this.employeeId),
      this.employeeService.salaryHistory(this.employeeId),
    ]).subscribe({
      next: ([employee, history]) => {
        this.employee.set(employee);
        this.history.set(history);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openAdjustmentDialog(): void {
    const employee = this.employee();
    if (!employee) {
      return;
    }
    const ref = this.dialog.open(SalaryAdjustmentDialogComponent, {
      width: '420px',
      data: { employeeId: this.employeeId, currentCurrency: employee.currentSalaryCurrency },
    });
    ref.afterClosed().subscribe((created) => {
      if (created) {
        this.snackBar.open(
          `Salary updated to ${created.amount.toLocaleString()} ${created.currencyCode}, effective ${created.effectiveDate}`,
          'Dismiss',
          { duration: 5000 },
        );
        this.load();
      }
    });
  }

  backToDirectory(): void {
    this.router.navigate(['/employees']);
  }
}
