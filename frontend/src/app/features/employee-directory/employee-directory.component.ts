import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Employee, EmployeeFilter, EmployeeSort } from '../../core/models/employee.model';
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
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatTooltipModule,
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

  /** Maps a mat-sort column id to the backend Sort property it corresponds to. */
  private readonly sortPropertyByColumn: Record<string, string> = {
    name: 'lastName',
    department: 'department.name',
    country: 'country.name',
    jobBand: 'jobBand',
    salary: 'currentSalaryAmount',
    status: 'status',
  };

  readonly loading = signal(false);
  readonly exporting = signal(false);
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
  private sort: EmployeeSort | null = null;

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

    this.employeeService.list(filter, this.page, this.pageSize, this.sort).subscribe({
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

  onSortChange(sort: Sort): void {
    const property = this.sortPropertyByColumn[sort.active];
    this.sort = sort.direction && property ? { property, direction: sort.direction } : null;
    this.page = 0;
    this.load();
  }

  openEmployee(id: number): void {
    this.router.navigate(['/employees', id]);
  }

  exportCsv(): void {
    const raw = this.filterForm.getRawValue();
    const filter: EmployeeFilter = {
      search: raw.search || undefined,
      departmentId: raw.departmentId ?? undefined,
      countryCode: raw.countryCode || undefined,
      jobBand: raw.jobBand || undefined,
    };

    this.exporting.set(true);
    this.employeeService.exportCsv(filter).subscribe({
      next: (blob) => {
        this.exporting.set(false);
        this.downloadBlob(blob, 'employees.csv');
      },
      error: () => this.exporting.set(false),
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
