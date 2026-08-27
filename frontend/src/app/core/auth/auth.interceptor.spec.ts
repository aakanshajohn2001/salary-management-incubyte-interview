import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let navigateSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    navigateSpy = TestBed.inject(Router).navigate as unknown as ReturnType<typeof vi.fn>;
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('attaches the bearer token when a session exists', () => {
    authService.login({ username: 'hr.manager', password: 'pw' }).subscribe();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ token: 'my-token', username: 'hr.manager', role: 'HR_MANAGER', expiresAt: '2030-01-01T00:00:00Z' });

    http.get(`${environment.apiBaseUrl}/employees`).subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/employees`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
    req.flush({});
  });

  it('does not attach a header when there is no session', () => {
    http.get(`${environment.apiBaseUrl}/employees`).subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/employees`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('logs out and redirects to /login on a 401 response', () => {
    authService.login({ username: 'hr.manager', password: 'pw' }).subscribe();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ token: 'my-token', username: 'hr.manager', role: 'HR_MANAGER', expiresAt: '2030-01-01T00:00:00Z' });

    http.get(`${environment.apiBaseUrl}/employees`).subscribe({ error: () => {} });
    httpMock
      .expectOne(`${environment.apiBaseUrl}/employees`)
      .flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authService.isAuthenticated()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('does not log out or redirect on a non-401 error', () => {
    authService.login({ username: 'hr.manager', password: 'pw' }).subscribe();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ token: 'my-token', username: 'hr.manager', role: 'HR_MANAGER', expiresAt: '2030-01-01T00:00:00Z' });

    http.get(`${environment.apiBaseUrl}/employees`).subscribe({ error: () => {} });
    httpMock
      .expectOne(`${environment.apiBaseUrl}/employees`)
      .flush({ message: 'boom' }, { status: 500, statusText: 'Internal Server Error' });

    expect(authService.isAuthenticated()).toBe(true);
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
