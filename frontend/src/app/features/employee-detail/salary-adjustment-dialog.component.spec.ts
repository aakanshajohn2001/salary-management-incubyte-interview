import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNativeDateAdapter } from '@angular/material/core';
import { environment } from '../../../environments/environment';
import { SalaryAdjustmentDialogComponent } from './salary-adjustment-dialog.component';

describe('SalaryAdjustmentDialogComponent', () => {
  let httpMock: HttpTestingController;
  let dialogRefStub: { close: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    dialogRefStub = { close: vi.fn() };

    TestBed.configureTestingModule({
      imports: [SalaryAdjustmentDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MatDialogRef, useValue: dialogRefStub },
        { provide: MAT_DIALOG_DATA, useValue: { employeeId: 42, currentCurrency: 'USD' } },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('does not submit when the form is invalid', () => {
    const fixture = TestBed.createComponent(SalaryAdjustmentDialogComponent);
    fixture.detectChanges();

    fixture.componentInstance.submit();
    fixture.detectChanges();

    httpMock.expectNone(() => true);
    expect(fixture.componentInstance.form.touched).toBe(true);
    expect(dialogRefStub.close).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('required');
  });

  it('posts the adjustment and closes the dialog with the created entry on success', () => {
    const fixture = TestBed.createComponent(SalaryAdjustmentDialogComponent);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      amount: 165000,
      effectiveDate: new Date(2026, 0, 1),
      reason: 'Annual raise',
    });
    fixture.componentInstance.submit();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/employees/42/salary-adjustments`);
    expect(req.request.body).toEqual({ amount: 165000, effectiveDate: '2026-01-01', reason: 'Annual raise' });
    const created = { id: 1, amount: 165000, currencyCode: 'USD', effectiveDate: '2026-01-01', reason: 'Annual raise', createdAt: '2026-01-01T00:00:00Z' };
    req.flush(created);

    expect(dialogRefStub.close).toHaveBeenCalledWith(created);
    expect(fixture.componentInstance.errorMessage()).toBeNull();
  });

  it('surfaces the backend error message and does not close on failure', () => {
    const fixture = TestBed.createComponent(SalaryAdjustmentDialogComponent);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      amount: 100,
      effectiveDate: new Date(2019, 0, 1),
      reason: 'Too early',
    });
    fixture.componentInstance.submit();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/employees/42/salary-adjustments`);
    req.flush(
      { message: "Effective date cannot be before the employee's hire date" },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(dialogRefStub.close).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage()).toBe("Effective date cannot be before the employee's hire date");
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain("Effective date cannot be before the employee's hire date");
  });

  it('cancel closes the dialog with false', () => {
    const fixture = TestBed.createComponent(SalaryAdjustmentDialogComponent);
    fixture.detectChanges();

    fixture.componentInstance.cancel();

    expect(dialogRefStub.close).toHaveBeenCalledWith(false);
  });
});
