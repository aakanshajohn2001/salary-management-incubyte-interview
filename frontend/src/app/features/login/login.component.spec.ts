import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let httpMock: HttpTestingController;
  let navigateSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    localStorage.clear();
    navigateSpy = vi.fn();

    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: navigateSpy } },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('does not submit when the form is invalid', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    fixture.componentInstance.submit();

    httpMock.expectNone(() => true);
  });

  it('navigates to /employees on successful login', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({ username: 'hr.manager', password: 'Secret123!' });
    fixture.componentInstance.submit();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ token: 't', username: 'hr.manager', role: 'HR_MANAGER', expiresAt: '2030-01-01T00:00:00Z' });

    expect(navigateSpy).toHaveBeenCalledWith(['/employees']);
    expect(fixture.componentInstance.errorMessage()).toBeNull();
  });

  it('shows an error message on invalid credentials and does not navigate', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({ username: 'hr.manager', password: 'wrong' });
    fixture.componentInstance.submit();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ message: 'Invalid username or password' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage()).toBe('Invalid username or password.');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Invalid username or password.');
  });

  it('shows a loading spinner instead of "Sign in" while the request is in flight', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({ username: 'hr.manager', password: 'Secret123!' });
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Sign in');

    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ token: 't', username: 'hr.manager', role: 'HR_MANAGER', expiresAt: '2030-01-01T00:00:00Z' });
  });

  it('shows a generic error message on a non-401 failure (e.g. server error)', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({ username: 'hr.manager', password: 'Secret123!' });
    fixture.componentInstance.submit();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ message: 'boom' }, { status: 500, statusText: 'Internal Server Error' });

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage()).toBe('Something went wrong. Please try again.');
  });
});
