export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface ValidationErrorDetail {
  field: string;
  rejectedValue?: string;
  message: string;
}

export interface ErrorResponse {
  success?: boolean;
  errorCode: string;
  message: string;
  path?: string;
  correlationId?: string;
  timestamp?: string;
  validationErrors?: ValidationErrorDetail[];
}
