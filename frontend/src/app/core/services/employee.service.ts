import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Employee, EmployeeFilter, EmployeeSort } from '../models/employee.model';
import { PageResponse } from '../models/page-response.model';
import { SalaryHistoryEntry } from '../models/salary.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/employees`;

  list(
    filter: EmployeeFilter,
    page: number,
    size: number,
    sort?: EmployeeSort | null,
  ): Observable<PageResponse<Employee>> {
    let params = this.buildParams(filter, page, size);
    if (sort) {
      params = params.set('sort', `${sort.property},${sort.direction}`);
    }
    return this.http.get<PageResponse<Employee>>(this.baseUrl, { params });
  }

  get(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/${id}`);
  }

  salaryHistory(id: number): Observable<SalaryHistoryEntry[]> {
    return this.http.get<SalaryHistoryEntry[]>(`${this.baseUrl}/${id}/salary-history`);
  }

  /**
   * Fetched through HttpClient (not a plain link/window.open) so the auth
   * interceptor attaches the bearer token -- a direct navigation couldn't
   * carry an Authorization header.
   */
  exportCsv(filter: EmployeeFilter): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/export`, {
      params: this.buildFilterParams(filter),
      responseType: 'blob',
    });
  }

  private buildParams(filter: EmployeeFilter, page: number, size: number): HttpParams {
    return this.buildFilterParams(filter).set('page', page).set('size', size);
  }

  private buildFilterParams(filter: EmployeeFilter): HttpParams {
    let params = new HttpParams();
    if (filter.search) {
      params = params.set('search', filter.search);
    }
    if (filter.departmentId != null) {
      params = params.set('departmentId', filter.departmentId);
    }
    if (filter.countryCode) {
      params = params.set('countryCode', filter.countryCode);
    }
    if (filter.jobBand) {
      params = params.set('jobBand', filter.jobBand);
    }
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    return params;
  }
}
