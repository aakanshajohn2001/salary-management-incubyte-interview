import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { EmployeeDirectoryComponent } from './employee-directory.component';

describe('EmployeeDirectoryComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EmployeeDirectoryComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function flushBootstrapRequests(): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/reference/departments`).flush([{ id: 1, name: 'Engineering' }]);
    httpMock.expectOne(`${environment.apiBaseUrl}/reference/countries`).flush([{ code: 'US', name: 'United States' }]);
    httpMock
      .expectOne((req) => req.url === `${environment.apiBaseUrl}/employees`)
      .flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  }

  it('loads reference data and the first page of employees on init', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();

    expect(fixture.componentInstance.departments()).toEqual([{ id: 1, name: 'Engineering' }]);
    expect(fixture.componentInstance.countries()).toEqual([{ code: 'US', name: 'United States' }]);
    expect(fixture.componentInstance.pageData()?.totalElements).toBe(0);
  });

  it('load() sends the current filter form values as query params', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();

    fixture.componentInstance.filterForm.patchValue({ search: 'lovelace', departmentId: 1, countryCode: 'US' });
    fixture.componentInstance.load();

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/employees` &&
        r.params.get('search') === 'lovelace' &&
        r.params.get('departmentId') === '1' &&
        r.params.get('countryCode') === 'US',
    );
    req.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('onPageChange reloads with the new page index and size', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();

    fixture.componentInstance.onPageChange({ pageIndex: 2, pageSize: 50 } as any);

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/employees` && r.params.get('page') === '2' && r.params.get('size') === '50',
    );
    req.flush({ content: [], page: 2, size: 50, totalElements: 0, totalPages: 0 });
  });
});
