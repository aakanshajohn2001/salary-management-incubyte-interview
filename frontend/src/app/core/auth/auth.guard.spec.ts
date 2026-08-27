import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let authServiceStub: { isAuthenticated: ReturnType<typeof vi.fn> };
  let routerStub: { createUrlTree: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authServiceStub = { isAuthenticated: vi.fn() };
    routerStub = { createUrlTree: vi.fn().mockReturnValue('the-url-tree' as unknown as UrlTree) };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceStub },
        { provide: Router, useValue: routerStub },
      ],
    });
  });

  it('allows navigation when authenticated', () => {
    authServiceStub.isAuthenticated.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

    expect(result).toBe(true);
    expect(routerStub.createUrlTree).not.toHaveBeenCalled();
  });

  it('redirects to /login when not authenticated', () => {
    authServiceStub.isAuthenticated.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

    expect(routerStub.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(result).toBe('the-url-tree');
  });
});
