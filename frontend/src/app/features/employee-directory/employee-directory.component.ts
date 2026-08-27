import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Employee, EmployeeFilter } from '../../core/models/employee.model';
import { PageResponse } from '../../core/models/page-response.model';
import { Country, Department, JOB_BANDS } from '../../core/models/reference.model';
import { EmployeeService } from '../../core/services/employee.service';
import { ReferenceDataService } from '../../core/services/reference-data.service';

@Component({
  selector: 'app-employee-directory',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DecimalPipe,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
  templateUrl: './employee-directory.component.html',
  styleUrl: './employee-directory.component.scss',
})
export class EmployeeDirectoryComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly referenceDataService = inject(ReferenceDataService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly displayedColumns = ['name', 'department', 'country', 'jobBand', 'salary', 'status'];
  readonly jobBands = JOB_BANDS;

  readonly loading = signal(false);
  readonly pageData = signal<PageResponse<Employee> | null>(null);
  readonly departments = signal<Department[]>([]);
  readonly countries = signal<Country[]>([]);

  readonly filterForm = this.fb.nonNullable.group({
    search: [''],
    departmentId: [null as number | null],
    countryCode: [''],
    jobBand: [''],
  });

  private page = 0;
  private pageSize = 25;

  ngOnInit(): void {
    this.referenceDataService.departments().subscribe((departments) => this.departments.set(departments));
    this.referenceDataService.countries().subscribe((countries) => this.countries.set(countries));

    this.filterForm.valueChanges.pipe(debounceTime(300)).subscribe(() => {
      this.page = 0;
      this.load();
    });

    this.load();
  }

  load(): void {
    this.loading.set(true);
    const raw = this.filterForm.getRawValue();
    const filter: EmployeeFilter = {
      search: raw.search || undefined,
      departmentId: raw.departmentId ?? undefined,
      countryCode: raw.countryCode || undefined,
      jobBand: raw.jobBand || undefined,
    };

    this.employeeService.list(filter, this.page, this.pageSize).subscribe({
      next: (data) => {
        this.pageData.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPageChange(event: PageEvent): void {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  openEmployee(id: number): void {
    this.router.navigate(['/employees', id]);
  }
}
