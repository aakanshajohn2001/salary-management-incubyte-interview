import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SalaryAdjustmentRequest, SalaryHistoryEntry } from '../models/salary.model';

@Injectable({ providedIn: 'root' })
export class SalaryService {
  private readonly http = inject(HttpClient);

  addAdjustment(employeeId: number, request: SalaryAdjustmentRequest): Observable<SalaryHistoryEntry> {
    return this.http.post<SalaryHistoryEntry>(
      `${environment.apiBaseUrl}/employees/${employeeId}/salary-adjustments`,
      request,
    );
  }
}
