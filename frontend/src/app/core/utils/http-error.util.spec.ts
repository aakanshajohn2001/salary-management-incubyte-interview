import { HttpErrorResponse } from '@angular/common/http';
import { extractErrorMessage } from './http-error.util';

describe('extractErrorMessage', () => {
  it('returns the backend message when the error is an HttpErrorResponse with an ApiError body', () => {
    const error = new HttpErrorResponse({ error: { message: 'Invalid username or password' }, status: 401 });
    expect(extractErrorMessage(error, 'fallback')).toBe('Invalid username or password');
  });

  it('returns the fallback when the error body has no message field', () => {
    const error = new HttpErrorResponse({ error: {}, status: 500 });
    expect(extractErrorMessage(error, 'fallback')).toBe('fallback');
  });

  it('returns the fallback when the error is not an HttpErrorResponse at all', () => {
    expect(extractErrorMessage(new Error('network down'), 'fallback')).toBe('fallback');
    expect(extractErrorMessage(null, 'fallback')).toBe('fallback');
  });
});
