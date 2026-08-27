import { Component, HostListener, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

const WIDTH_STORAGE_KEY = 'sidebar_width';
const COLLAPSED_STORAGE_KEY = 'sidebar_collapsed';
const MIN_WIDTH = 200;
const MAX_WIDTH = 400;
const DEFAULT_WIDTH = 260;
const RESIZE_STEP = 16;

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  readonly sidebarWidth = signal(this.readStoredWidth());
  readonly sidebarCollapsed = signal(this.readStoredCollapsed());
  readonly isResizing = signal(false);

  private dragging = false;

  constructor(
    readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  toggleSidebar(): void {
    const collapsed = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(collapsed);
    localStorage.setItem(COLLAPSED_STORAGE_KEY, String(collapsed));
  }

  startResize(event: MouseEvent): void {
    event.preventDefault();
    this.dragging = true;
    this.isResizing.set(true);
    document.body.style.userSelect = 'none';
    document.body.style.cursor = 'col-resize';
  }

  onResizeKeydown(event: KeyboardEvent): void {
    let width = this.sidebarWidth();
    if (event.key === 'ArrowLeft') {
      width -= RESIZE_STEP;
    } else if (event.key === 'ArrowRight') {
      width += RESIZE_STEP;
    } else {
      return;
    }
    event.preventDefault();
    this.setWidth(width);
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    if (!this.dragging) {
      return;
    }
    this.sidebarWidth.set(this.clampWidth(event.clientX));
  }

  @HostListener('document:mouseup')
  onMouseUp(): void {
    if (!this.dragging) {
      return;
    }
    this.dragging = false;
    this.isResizing.set(false);
    document.body.style.userSelect = '';
    document.body.style.cursor = '';
    localStorage.setItem(WIDTH_STORAGE_KEY, String(this.sidebarWidth()));
  }

  private setWidth(width: number): void {
    const clamped = this.clampWidth(width);
    this.sidebarWidth.set(clamped);
    localStorage.setItem(WIDTH_STORAGE_KEY, String(clamped));
  }

  private clampWidth(width: number): number {
    return Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, width));
  }

  private readStoredWidth(): number {
    const raw = Number(localStorage.getItem(WIDTH_STORAGE_KEY));
    return Number.isFinite(raw) && raw > 0 ? this.clampWidth(raw) : DEFAULT_WIDTH;
  }

  private readStoredCollapsed(): boolean {
    return localStorage.getItem(COLLAPSED_STORAGE_KEY) === 'true';
  }
}
