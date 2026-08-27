import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthService } from '../../core/auth/auth.service';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  let logoutSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    logoutSpy = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: '', component: ShellComponent },
          { path: 'login', component: ShellComponent },
        ]),
        { provide: AuthService, useValue: { logout: logoutSpy, username: () => 'hr.manager' } },
      ],
    });
  });

  it('renders the current username in the toolbar', async () => {
    const harness = await RouterTestingHarness.create('/');
    await harness.navigateByUrl('/', ShellComponent);
    harness.detectChanges();

    expect((harness.routeNativeElement as HTMLElement).textContent).toContain('hr.manager');
  });

  it('logout clears the session and navigates to /login', async () => {
    const harness = await RouterTestingHarness.create('/');
    const component = await harness.navigateByUrl('/', ShellComponent);

    component.logout();
    await harness.fixture.whenStable();

    expect(logoutSpy).toHaveBeenCalled();
    expect(TestBed.inject(Router).url).toBe('/login');
  });
});
