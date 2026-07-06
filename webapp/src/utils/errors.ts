import axios from 'axios';

interface ErrorResponseBody {
  message?: string;
  error?: string;
  detail?: string;
}

export function getApiErrorMessage(error: unknown, fallback = 'Произошла ошибка'): string {
  if (axios.isAxiosError<ErrorResponseBody>(error)) {
    const data = error.response?.data;
    return data?.message ?? data?.detail ?? data?.error ?? error.message ?? fallback;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}
