export function getCsrfToken(): string {
  if (typeof document === 'undefined') return '';
  const value = `; ${document.cookie}`;
  const parts = value.split(`; XSRF-TOKEN=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift() || '';
  return '';
}

export async function apiFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const headers = new Headers(init?.headers);
  const token = getCsrfToken();
  if (token) {
    headers.set('X-XSRF-TOKEN', token);
  }
  return fetch(input, {
    ...init,
    headers,
    credentials: 'include',
  });
}
