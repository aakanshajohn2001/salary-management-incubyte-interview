import { HttpErrorResponse } from '@angular/common/http';

/** Backend errors follow the ApiError shape: { message: string, ... }. */
export function extractErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
    return error.error.message;
  }
  return fallback;
}
