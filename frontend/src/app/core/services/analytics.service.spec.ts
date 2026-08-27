import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AnalyticsService } from './analytics.service';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the analytics summary from the correct endpoint', () => {
    service.getSummary().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/analytics/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalHeadcount: 0, totalPayrollUsd: 0, averageSalaryUsd: 0, byDepartment: [], byCountry: [], byJobBand: [] });
  });
});
