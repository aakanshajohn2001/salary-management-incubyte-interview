import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalyticsSummary, RecentSalaryChange } from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  getSummary(): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>(`${environment.apiBaseUrl}/analytics/summary`);
  }

  getRecentChanges(limit = 10): Observable<RecentSalaryChange[]> {
    return this.http.get<RecentSalaryChange[]>(`${environment.apiBaseUrl}/analytics/recent-changes`, {
      params: new HttpParams().set('limit', limit),
    });
  }
}
