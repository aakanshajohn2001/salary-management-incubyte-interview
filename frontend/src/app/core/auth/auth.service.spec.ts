import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts unauthenticated when nothing is stored', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
  });

  it('login stores the session and marks the user authenticated', () => {
    service.login({ username: 'hr.manager', password: 'Secret123!' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush({
      token: 'fake-jwt',
      username: 'hr.manager',
      role: 'HR_MANAGER',
      expiresAt: '2030-01-01T00:00:00Z',
    });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('fake-jwt');
    expect(service.username()).toBe('hr.manager');
    expect(localStorage.getItem('salary_auth')).toContain('fake-jwt');
  });

  it('logout clears the session', () => {
    service.login({ username: 'hr.manager', password: 'Secret123!' }).subscribe();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ token: 't', username: 'hr.manager', role: 'HR_MANAGER', expiresAt: '2030-01-01T00:00:00Z' });

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
    expect(localStorage.getItem('salary_auth')).toBeNull();
  });

  it('restores a previously stored session on construction', () => {
    localStorage.setItem(
      'salary_auth',
      JSON.stringify({ token: 'stored-jwt', username: 'hr.manager', role: 'HR_MANAGER', expiresAt: '2030-01-01T00:00:00Z' }),
    );

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const freshService = TestBed.inject(AuthService);

    expect(freshService.isAuthenticated()).toBe(true);
    expect(freshService.token()).toBe('stored-jwt');
  });
});
