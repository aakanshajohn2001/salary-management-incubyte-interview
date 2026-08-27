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

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  function flushBootstrapRequests(): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/reference/departments`).flush([{ id: 1, name: 'Engineering' }]);
    httpMock.expectOne(`${environment.apiBaseUrl}/reference/countries`).flush([{ code: 'US', name: 'United States' }]);
    httpMock
      .expectOne((req) => req.url === `${environment.apiBaseUrl}/employees`)
      .flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  }

  it('loads reference data and the first page of employees on init, and renders the empty state', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();
    fixture.detectChanges();

    expect(fixture.componentInstance.departments()).toEqual([{ id: 1, name: 'Engineering' }]);
    expect(fixture.componentInstance.countries()).toEqual([{ code: 'US', name: 'United States' }]);
    expect(fixture.componentInstance.pageData()?.totalElements).toBe(0);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No employees match these filters');
  });

  it('renders a row per employee with the current salary formatted', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/reference/departments`).flush([{ id: 1, name: 'Engineering' }]);
    httpMock.expectOne(`${environment.apiBaseUrl}/reference/countries`).flush([{ code: 'US', name: 'United States' }]);
    httpMock.expectOne((req) => req.url === `${environment.apiBaseUrl}/employees`).flush({
      content: [
        {
          id: 1,
          firstName: 'Ada',
          lastName: 'Lovelace',
          email: 'ada@acme.example',
          department: 'Engineering',
          countryCode: 'US',
          countryName: 'United States',
          jobBand: 'L5',
          hireDate: '2020-01-01',
          status: 'ACTIVE',
          currentSalaryAmount: 165000,
          currentSalaryCurrency: 'USD',
          currentSalaryEffectiveDate: '2022-01-01',
          belowBandAverage: false,
        },
      ],
      page: 0,
      size: 25,
      totalElements: 1,
      totalPages: 1,
    });
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Ada');
    expect(text).toContain('165,000.00');
    expect(text).toContain('USD');
  });

  it('shows the below-band-average flag only for employees flagged as an outlier', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/reference/departments`).flush([{ id: 1, name: 'Engineering' }]);
    httpMock.expectOne(`${environment.apiBaseUrl}/reference/countries`).flush([{ code: 'US', name: 'United States' }]);
    httpMock.expectOne((req) => req.url === `${environment.apiBaseUrl}/employees`).flush({
      content: [
        {
          id: 1,
          firstName: 'Ada',
          lastName: 'Lovelace',
          email: 'ada@acme.example',
          department: 'Engineering',
          countryCode: 'US',
          countryName: 'United States',
          jobBand: 'L5',
          hireDate: '2020-01-01',
          status: 'ACTIVE',
          currentSalaryAmount: 60000,
          currentSalaryCurrency: 'USD',
          currentSalaryEffectiveDate: '2022-01-01',
          belowBandAverage: true,
        },
        {
          id: 2,
          firstName: 'Grace',
          lastName: 'Hopper',
          email: 'grace@acme.example',
          department: 'Engineering',
          countryCode: 'US',
          countryName: 'United States',
          jobBand: 'L5',
          hireDate: '2020-01-01',
          status: 'ACTIVE',
          currentSalaryAmount: 200000,
          currentSalaryCurrency: 'USD',
          currentSalaryEffectiveDate: '2022-01-01',
          belowBandAverage: false,
        },
      ],
      page: 0,
      size: 25,
      totalElements: 2,
      totalPages: 1,
    });
    fixture.detectChanges();

    const flags = (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="below-band-average-flag"]');
    expect(flags.length).toBe(1);
  });

  it('onSortChange maps the clicked column to its backend sort property and reloads', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();

    fixture.componentInstance.onSortChange({ active: 'salary', direction: 'desc' });

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/employees` && r.params.get('sort') === 'currentSalaryAmount,desc',
    );
    req.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('onSortChange clears the sort when the column cycles back to unsorted', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();

    fixture.componentInstance.onSortChange({ active: 'salary', direction: 'desc' });
    httpMock
      .expectOne((r) => r.url === `${environment.apiBaseUrl}/employees`)
      .flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });

    fixture.componentInstance.onSortChange({ active: 'salary', direction: '' });

    const req = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/employees`);
    expect(req.request.params.has('sort')).toBe(false);
    req.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
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

  it('load() also forwards the jobBand filter as a query param', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();

    fixture.componentInstance.filterForm.patchValue({ jobBand: 'L5' });
    fixture.componentInstance.load();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/employees` && r.params.get('jobBand') === 'L5',
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

  it('exportCsv requests a blob with the current filters and triggers a download', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();

    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:fake-url');
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    const clickSpy = vi.fn();
    const realCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string, options?: ElementCreationOptions) => {
      if (tagName === 'a') {
        return { set href(_v: string) {}, set download(_v: string) {}, click: clickSpy } as unknown as HTMLAnchorElement;
      }
      return realCreateElement(tagName, options);
    });

    fixture.componentInstance.filterForm.patchValue({ search: 'lovelace' });
    fixture.componentInstance.exportCsv();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/employees/export` && r.params.get('search') === 'lovelace',
    );
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['csv-content']));

    expect(fixture.componentInstance.exporting()).toBe(false);
    expect(clickSpy).toHaveBeenCalled();
    expect(createObjectURL).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake-url');
  });

  it('exportCsv stops the exporting indicator on failure', () => {
    const fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    fixture.detectChanges();
    flushBootstrapRequests();

    fixture.componentInstance.exportCsv();

    httpMock
      .expectOne((r) => r.url === `${environment.apiBaseUrl}/employees/export`)
      .flush(new Blob(['boom']), { status: 500, statusText: 'Internal Server Error' });

    expect(fixture.componentInstance.exporting()).toBe(false);
  });
});
