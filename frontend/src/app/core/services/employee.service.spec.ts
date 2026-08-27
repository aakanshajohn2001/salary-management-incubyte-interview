import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { EmployeeService } from './employee.service';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EmployeeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list() forwards the status filter as a query param (not currently exposed in the directory UI)', () => {
    service.list({ status: 'TERMINATED' }, 0, 25).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/employees` && r.params.get('status') === 'TERMINATED',
    );
    req.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('get() fetches a single employee by id', () => {
    service.get(42).subscribe();
    httpMock.expectOne(`${environment.apiBaseUrl}/employees/42`).flush({});
  });

  it('salaryHistory() fetches the history for an employee', () => {
    service.salaryHistory(42).subscribe();
    httpMock.expectOne(`${environment.apiBaseUrl}/employees/42/salary-history`).flush([]);
  });
});
