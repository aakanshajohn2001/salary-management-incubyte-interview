import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { environment } from '../../../environments/environment';
import { AnalyticsSummary } from '../../core/models/analytics.model';
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

    expect(fixture.componentInstance.headcountByDepartment()).toEqual([{ name: 'Engineering', value: 2 }]);
    expect(fixture.componentInstance.payrollByCountry()).toEqual([
      { name: 'United States', value: 100000 },
      { name: 'India', value: 12000 },
    ]);
    expect(fixture.componentInstance.averageSalaryByBand()).toEqual([{ name: 'L4', value: 56000 }]);
  });

  it('stops loading and leaves summary null on a failed request', () => {
    const fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/analytics/summary`)
      .flush({ message: 'boom' }, { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.componentInstance.summary()).toBeNull();
  });
});
