import * as SecureStore from 'expo-secure-store';
import { ApiResponse, ErrorResponse } from '../types/common';

export const TOKEN_KEY = 'aarogya_jwt_token';

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

// Global listener for 401 Unauthorized events
type UnauthorizedListener = () => void;
const unauthorizedListeners: Set<UnauthorizedListener> = new Set();

export const onUnauthorized = (listener: UnauthorizedListener): (() => void) => {
  unauthorizedListeners.add(listener);
  return () => unauthorizedListeners.delete(listener);
};

export const tokenStorage = {
  get: async (): Promise<string | null> => {
    try {
      return await SecureStore.getItemAsync(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set: async (token: string): Promise<void> => {
    try {
      await SecureStore.setItemAsync(TOKEN_KEY, token);
    } catch (e) {
      console.warn('Failed to save JWT token to SecureStore', e);
    }
  },
  clear: async (): Promise<void> => {
    try {
      await SecureStore.deleteItemAsync(TOKEN_KEY);
    } catch (e) {
      console.warn('Failed to remove JWT token from SecureStore', e);
    }
  },
};

export interface ApiRequestOptions extends RequestInit {
  timeoutMs?: number;
}

export async function request<T>(baseUrl: string, endpoint: string, options: ApiRequestOptions = {}): Promise<T> {
  const url = `${baseUrl.replace(/\/+$/, '')}/${endpoint.replace(/^\/+/, '')}`;

  const headers = new Headers(options.headers || {});

  // Attach JWT Bearer Token if available
  const token = await tokenStorage.get();
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  // Detect FormData bodies robustly in React Native
  const isFormData =
    (typeof FormData !== 'undefined' && options.body instanceof FormData) ||
    Boolean(
      options.body &&
        typeof options.body === 'object' &&
        ((options.body as any)._parts !== undefined || options.body?.constructor?.name === 'FormData')
    );

  // Set JSON Content-Type only if not FormData (React Native fetch automatically sets boundary for FormData)
  if (isFormData) {
    headers.delete('Content-Type');
  } else if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  headers.set('Accept', 'application/json');

  // Default 15-second AbortController timeout (configurable up to 45s for file/OCR uploads)
  const timeoutMs = options.timeoutMs ?? 15000;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  let response: Response;
  try {
    // Explicitly use React Native runtime global fetch
    const fetchFn = typeof globalThis.fetch === 'function' ? globalThis.fetch : fetch;
    response = await fetchFn(url, {
      ...options,
      headers,
      signal: controller.signal,
    });
  } catch (err: any) {
    if (err?.name === 'AbortError') {
      const sec = Math.round(timeoutMs / 1000);
      console.error(`[API TIMEOUT] ${options.method || 'GET'} ${url} - status: 408`);
      throw new ApiError(
        `Request timed out (${sec}s). Please check your connection to the server.`,
        'TIMEOUT_ERROR',
        408
      );
    }
    if (err instanceof ApiError) {
      throw err;
    }
    const realMsg = err?.message ? String(err.message) : 'Network error';
    console.error(`[API NETWORK ERROR] ${options.method || 'GET'} ${url} - error:`, realMsg);
    throw new ApiError(
      `Network request failed: ${realMsg}`,
      'NETWORK_ERROR',
      0
    );
  } finally {
    clearTimeout(timeoutId);
  }

  // Handle 204 No Content
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
    console.error(
      `[API ERROR] ${options.method || 'GET'} ${url} | Status: ${response.status} | Body:`,
      JSON.stringify(body)
    );
    if (response.status === 401) {
      unauthorizedListeners.forEach((fn) => fn());
    }

    const errBody = body as ErrorResponse;
    const message =
      errBody?.message ||
      (body && typeof body === 'object' && body.error) ||
      response.statusText ||
      'An unexpected error occurred';
    const errorCode = errBody?.errorCode || `HTTP_${response.status}`;
    throw new ApiError(message, errorCode, response.status, errBody?.validationErrors);
  }

  // Return the data field from ApiResponse<T> if wrapped, else return body directly
  if (body && typeof body === 'object' && 'data' in body && (body as ApiResponse<T>).data !== undefined) {
    return (body as ApiResponse<T>).data;
  }

  return body as T;
}

/**
 * Uploads multipart/form-data via native XMLHttpRequest to prevent Expo SDK 57 WHATWG fetch
 * "Unsupported FormDataPart implementation" errors with React Native { uri, name, type } objects.
 */
export async function uploadMultipart<T>(
  baseUrl: string,
  endpoint: string,
  formData: FormData,
  timeoutMs = 45000
): Promise<T> {
  const url = `${baseUrl.replace(/\/+$/, '')}/${endpoint.replace(/^\/+/, '')}`;
  const token = await tokenStorage.get();

  return new Promise<T>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', url);
    xhr.timeout = timeoutMs;

    // Attach JWT Authorization if available
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
    // Accept JSON response
    xhr.setRequestHeader('Accept', 'application/json');
    // Note: Do NOT set Content-Type header so XMLHttpRequest sets the multipart/form-data boundary automatically

    xhr.onload = () => {
      let body: any = null;
      try {
        body = xhr.responseText ? JSON.parse(xhr.responseText) : null;
      } catch {
        body = xhr.responseText ? { message: xhr.responseText } : null;
      }

      if (xhr.status >= 200 && xhr.status < 300) {
        if (body && typeof body === 'object' && 'data' in body && (body as ApiResponse<T>).data !== undefined) {
          resolve((body as ApiResponse<T>).data);
        } else {
          resolve(body as T);
        }
      } else {
        console.error(`[XHR UPLOAD ERROR] POST ${url} | Status: ${xhr.status} | Body:`, xhr.responseText);
        if (xhr.status === 401) {
          unauthorizedListeners.forEach((fn) => fn());
        }
        const errBody = body as ErrorResponse;
        const message =
          errBody?.message ||
          (body && typeof body === 'object' && body.error) ||
          xhr.statusText ||
          `Upload failed with status ${xhr.status}`;
        const errorCode = errBody?.errorCode || `HTTP_${xhr.status}`;
        reject(new ApiError(message, errorCode, xhr.status, errBody?.validationErrors));
      }
    };

    xhr.onerror = () => {
      console.error(`[XHR UPLOAD NETWORK ERROR] POST ${url}`);
      reject(new ApiError('Network request failed during multipart upload', 'NETWORK_ERROR', 0));
    };

    xhr.ontimeout = () => {
      const sec = Math.round(timeoutMs / 1000);
      console.error(`[XHR UPLOAD TIMEOUT] POST ${url} - timeout: ${sec}s`);
      reject(new ApiError(`Upload request timed out (${sec}s)`, 'TIMEOUT_ERROR', 408));
    };

    xhr.send(formData as any);
  });
}

