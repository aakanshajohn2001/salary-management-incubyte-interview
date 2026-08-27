import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Employee } from '../../core/models/employee.model';
import { SalaryHistoryEntry } from '../../core/models/salary.model';
import { EmployeeDetailComponent } from './employee-detail.component';

const EMPLOYEE: Employee = {
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
};

const HISTORY: SalaryHistoryEntry[] = [
  { id: 2, amount: 165000, currencyCode: 'USD', effectiveDate: '2022-01-01', reason: 'Annual raise', createdAt: '2022-01-01T00:00:00Z' },
  { id: 1, amount: 150000, currencyCode: 'USD', effectiveDate: '2020-01-01', reason: 'Initial hire', createdAt: '2020-01-01T00:00:00Z' },
];

describe('EmployeeDetailComponent', () => {
  let httpMock: HttpTestingController;
  let navigateSpy: ReturnType<typeof vi.fn>;
  let dialogOpenSpy: ReturnType<typeof vi.fn>;
  let snackBarOpenSpy: ReturnType<typeof vi.fn>;

  function flushInitialLoad(): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/employees/1`).flush(EMPLOYEE);
    httpMock.expectOne(`${environment.apiBaseUrl}/employees/1/salary-history`).flush(HISTORY);
  }

  beforeEach(() => {
    navigateSpy = vi.fn();
    dialogOpenSpy = vi.fn();
    snackBarOpenSpy = vi.fn();

    TestBed.configureTestingModule({
      imports: [EmployeeDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        provideNoopAnimations(),
        { provide: Router, useValue: { navigate: navigateSpy } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } },
        },
      ],
    });
    // MatDialogModule/MatSnackBarModule (imported by the component itself)
    // provide their own services in a child injector that shadows a plain
    // `providers` override, so these need overrideProvider instead.
    TestBed.overrideProvider(MatDialog, { useValue: { open: dialogOpenSpy } });
    TestBed.overrideProvider(MatSnackBar, { useValue: { open: snackBarOpenSpy } });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads the employee profile and salary history on init, and renders them', () => {
    const fixture = TestBed.createComponent(EmployeeDetailComponent);
    fixture.detectChanges();
    flushInitialLoad();
    fixture.detectChanges();

    expect(fixture.componentInstance.employee()).toEqual(EMPLOYEE);
    expect(fixture.componentInstance.history()).toEqual(HISTORY);
    expect(fixture.componentInstance.loading()).toBe(false);

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Ada');
    expect(text).toContain('Lovelace');
    expect(text).toContain('Annual raise');
    expect(text).toContain('Initial hire');
  });

  it('opens the adjustment dialog with the employee id and currency, shows a snackbar, and reloads on success', () => {
    const fixture = TestBed.createComponent(EmployeeDetailComponent);
    fixture.detectChanges();
    flushInitialLoad();

    const created = {
      id: 3,
      amount: 180000,
      currencyCode: 'USD',
      effectiveDate: '2026-06-01',
      reason: 'Promotion',
      createdAt: '2026-06-01T00:00:00Z',
    };
    dialogOpenSpy.mockReturnValue({ afterClosed: () => of(created) });
    fixture.componentInstance.openAdjustmentDialog();

    expect(dialogOpenSpy).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ data: { employeeId: 1, currentCurrency: 'USD' } }),
    );
    expect(snackBarOpenSpy).toHaveBeenCalledWith(
      expect.stringContaining('180,000 USD'),
      'Dismiss',
      { duration: 5000 },
    );

    // afterClosed(created) triggers a reload
    flushInitialLoad();
  });

  it('does not reload when the adjustment dialog is cancelled', () => {
    const fixture = TestBed.createComponent(EmployeeDetailComponent);
    fixture.detectChanges();
    flushInitialLoad();

    dialogOpenSpy.mockReturnValue({ afterClosed: () => of(false) });
    fixture.componentInstance.openAdjustmentDialog();

    httpMock.expectNone(`${environment.apiBaseUrl}/employees/1`);
  });

  it('stops loading without crashing when the initial load fails', () => {
    const fixture = TestBed.createComponent(EmployeeDetailComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/employees/1`)
      .flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    // forkJoin may or may not have unsubscribed the sibling request by this
    // point depending on scheduling; only flush it if it's still open.
    httpMock.match(() => true).forEach((req) => {
      if (!req.cancelled) {
        req.flush([]);
      }
    });

    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.componentInstance.employee()).toBeNull();
  });

  it('openAdjustmentDialog is a no-op before the employee has loaded', () => {
    const fixture = TestBed.createComponent(EmployeeDetailComponent);
    fixture.detectChanges();
    // Note: no flushInitialLoad() -- employee() is still null here.

    fixture.componentInstance.openAdjustmentDialog();

    expect(dialogOpenSpy).not.toHaveBeenCalled();

    flushInitialLoad();
  });

  it('backToDirectory navigates to /employees', () => {
    const fixture = TestBed.createComponent(EmployeeDetailComponent);
    fixture.detectChanges();
    flushInitialLoad();

    fixture.componentInstance.backToDirectory();

    expect(navigateSpy).toHaveBeenCalledWith(['/employees']);
  });
});
