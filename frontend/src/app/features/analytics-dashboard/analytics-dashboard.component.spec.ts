import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { environment } from '../../../environments/environment';
import { AnalyticsSummary, RecentSalaryChange } from '../../core/models/analytics.model';
import { AnalyticsDashboardComponent } from './analytics-dashboard.component';

const SUMMARY: AnalyticsSummary = {
  totalHeadcount: 2,
  totalPayrollUsd: 112000,
  averageSalaryUsd: 56000,
  byDepartment: [{ department: 'Engineering', headcount: 2, totalPayrollUsd: 112000, averageSalaryUsd: 56000 }],
  byCountry: [
    { countryCode: 'US', countryName: 'United States', headcount: 1, totalPayrollUsd: 100000, averageSalaryUsd: 100000 },
    { countryCode: 'IN', countryName: 'India', headcount: 1, totalPayrollUsd: 12000, averageSalaryUsd: 12000 },
  ],
  byJobBand: [{ jobBand: 'L4', headcount: 2, minSalaryUsd: 12000, maxSalaryUsd: 100000, averageSalaryUsd: 56000 }],
};

const RECENT_CHANGES: RecentSalaryChange[] = [
  {
    employeeId: 1,
    employeeName: 'Ada Lovelace',
    department: 'Engineering',
    amount: 165000,
    currencyCode: 'USD',
    effectiveDate: '2022-01-01',
    reason: 'Annual raise',
    createdAt: '2022-01-01T00:00:00Z',
  },
];

function flushRecentChanges(httpMock: HttpTestingController, changes: RecentSalaryChange[] = RECENT_CHANGES): void {
  httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/analytics/recent-changes`).flush(changes);
}

describe('AnalyticsDashboardComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AnalyticsDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the summary on init, renders the stat tiles, and stops loading', () => {
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/analytics/summary`).flush(SUMMARY);
    flushRecentChanges(httpMock);
    fixture.detectChanges();

    expect(fixture.componentInstance.summary()).toEqual(SUMMARY);
    expect(fixture.componentInstance.loading()).toBe(false);

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('112,000');
    expect(text).toContain('56,000');
  });

  it('maps department/country/band data into chart-ready shapes', () => {
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/analytics/summary`).flush(SUMMARY);
    flushRecentChanges(httpMock);

    expect(fixture.componentInstance.headcountByDepartment()).toEqual([{ name: 'Engineering', value: 2 }]);
    expect(fixture.componentInstance.payrollByCountry()).toEqual([
      { name: 'United States', value: 100000 },
      { name: 'India', value: 12000 },
    ]);
    expect(fixture.componentInstance.averageSalaryByBand()).toEqual([{ name: 'L4', value: 56000 }]);
  });

  it('fetches and renders the recent salary changes feed', () => {
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/analytics/summary`).flush(SUMMARY);

    const req = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/analytics/recent-changes`);
    expect(req.request.params.get('limit')).toBe('10');
    req.flush(RECENT_CHANGES);
    fixture.detectChanges();

    expect(fixture.componentInstance.recentChanges()).toEqual(RECENT_CHANGES);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Ada Lovelace');
    expect(text).toContain('Annual raise');
  });

  it('shows an empty state when there are no recent salary changes', () => {
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/analytics/summary`).flush(SUMMARY);
    flushRecentChanges(httpMock, []);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('No salary adjustments have been recorded yet');
  });

  it('stops loading and leaves summary null on a failed request', () => {
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/analytics/summary`)
      .flush({ message: 'boom' }, { status: 500, statusText: 'Internal Server Error' });
    // forkJoin may or may not have unsubscribed the sibling request by this
    // point depending on scheduling; only flush it if it's still open.
    httpMock.match(() => true).forEach((req) => {
      if (!req.cancelled) {
        req.flush([]);
      }
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.componentInstance.summary()).toBeNull();
  });
});
