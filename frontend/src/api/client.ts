import { ApiResponse, ErrorResponse } from '../types/common';

const env = ((import.meta as any).env || {}) as Record<string, string | undefined>;
const BASE_URL = env.VITE_API_BASE_URL || '';
const TOKEN_KEY = 'aarogya_jwt_token';

function resolveUrl(endpoint: string): string {
  // In development (running with Vite dev server), always route using relative URLs
  // so Vite's proxy in vite.config.ts forwards requests cleanly to localhost without CORS errors
  if (import.meta.env.DEV) {
    return endpoint;
  }
  if (endpoint.startsWith('/api/doctors') && env.VITE_DOCTOR_SERVICE_URL) {
    return `${env.VITE_DOCTOR_SERVICE_URL.replace(/\/+$/, '')}${endpoint}`;
  }
  if (endpoint.startsWith('/api/consultations') && env.VITE_CONSULTATION_SERVICE_URL) {
    return `${env.VITE_CONSULTATION_SERVICE_URL.replace(/\/+$/, '')}${endpoint}`;
  }
  if (endpoint.startsWith('/api/communications') && env.VITE_COMMUNICATION_SERVICE_URL) {
    return `${env.VITE_COMMUNICATION_SERVICE_URL.replace(/\/+$/, '')}${endpoint}`;
  }
  if ((endpoint.startsWith('/api/v1/sos') || endpoint.startsWith('/api/v1/devices')) && env.VITE_SOS_SERVICE_URL) {
    return `${env.VITE_SOS_SERVICE_URL.replace(/\/+$/, '')}${endpoint}`;
  }
  if (endpoint.startsWith('/api/v1/chat') && env.VITE_CHAT_SERVICE_URL) {
    return `${env.VITE_CHAT_SERVICE_URL.replace(/\/+$/, '')}${endpoint}`;
  }
  if (
    (endpoint.startsWith('/api/v1/medications') ||
      endpoint.startsWith('/api/v1/medication-events') ||
      endpoint.startsWith('/api/v1/caregivers')) &&
    env.VITE_MEDICATION_SERVICE_URL
  ) {
    return `${env.VITE_MEDICATION_SERVICE_URL.replace(/\/+$/, '')}${endpoint}`;
  }
  if (
    (endpoint.startsWith('/api/v1/calls') ||
      endpoint.startsWith('/api/v1/follow-up') ||
      endpoint.startsWith('/api/v1/tools')) &&
    env.VITE_CALLING_SERVICE_URL
  ) {
    return `${env.VITE_CALLING_SERVICE_URL.replace(/\/+$/, '')}${endpoint}`;
  }
  return `${BASE_URL.replace(/\/+$/, '')}${endpoint}`;
}

export class ApiError extends Error {
  public errorCode: string;
  public status: number;
  public details?: any;

  constructor(message: string, errorCode: string = 'UNKNOWN_ERROR', status: number = 500, details?: any) {
    super(message);
    this.name = 'ApiError';
    this.errorCode = errorCode;
    this.status = status;
    this.details = details;
  }
}

export const tokenStorage = {
  get: (): string | null => {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set: (token: string): void => {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch (e) {
      console.error('Failed to save JWT token to localStorage', e);
    }
  },
  clear: (): void => {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch (e) {
      console.error('Failed to remove JWT token from localStorage', e);
    }
  },
};

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = resolveUrl(endpoint);

  const headers = new Headers(options.headers || {});

  // Attach JWT Bearer Token if available
  const token = tokenStorage.get();
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  // Set JSON Content-Type only if not FormData
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  headers.set('Accept', 'application/json');

  const config: RequestInit = {
    ...options,
    headers,
  };

  let response: Response;
  try {
    response = await fetch(url, config);
  } catch (err: any) {
    throw new ApiError(
      `Network connection failed: unable to reach healthcare service. Please verify the backend microservices are running.`,
      'NETWORK_ERROR',
      0
    );
  }

  // Handle empty responses
  if (response.status === 204) {
    return {} as T;
  }

  let body: any;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    try {
      body = await response.json();
    } catch {
      body = null;
    }
  } else {
    const text = await response.text();
    body = text ? { message: text } : null;
  }

  if (!response.ok) {
    // Notify on unauthorized
    if (response.status === 401) {
      window.dispatchEvent(new CustomEvent('aarogya:unauthorized'));
    }

    const errBody = body as ErrorResponse;
    const message = errBody?.message || (body && typeof body === 'object' && body.error) || response.statusText || 'An unexpected error occurred';
    const errorCode = errBody?.errorCode || `HTTP_${response.status}`;
    throw new ApiError(message, errorCode, response.status, errBody?.validationErrors);
  }

  // Return the data field from ApiResponse<T> if wrapped, else return body directly
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data;
  }

  return body as T;
}

export const apiClient = {
  get: <T>(endpoint: string, options?: RequestInit) =>
    request<T>(endpoint, { ...options, method: 'GET' }),

  post: <T>(endpoint: string, data?: any, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: data instanceof FormData ? data : data ? JSON.stringify(data) : undefined,
    }),

  put: <T>(endpoint: string, data?: any, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: data instanceof FormData ? data : data ? JSON.stringify(data) : undefined,
    }),

  patch: <T>(endpoint: string, data?: any, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'PATCH',
      body: data instanceof FormData ? data : data ? JSON.stringify(data) : undefined,
    }),

  delete: <T>(endpoint: string, options?: RequestInit) =>
    request<T>(endpoint, { ...options, method: 'DELETE' }),

  upload: <T>(endpoint: string, formData: FormData, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: formData,
    }),
};
