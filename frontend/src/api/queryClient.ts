import { useState, useEffect, useCallback, useRef } from 'react';

export interface QueryResult<T> {
  data: T | null;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export function useQuery<T>(
  queryFn: () => Promise<T>,
  deps: unknown[] = [],
  enabled: boolean = true
): QueryResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(enabled);
  const [isError, setIsError] = useState<boolean>(false);
  const [error, setError] = useState<Error | null>(null);

  const queryFnRef = useRef(queryFn);
  queryFnRef.current = queryFn;

  const execute = useCallback(async () => {
    if (!enabled) return;
    setIsLoading(true);
    setIsError(false);
    setError(null);
    try {
      const result = await queryFnRef.current();
      setData(result);
    } catch (err: any) {
      setIsError(true);
      setError(err instanceof Error ? err : new Error(String(err)));
    } finally {
      setIsLoading(false);
    }
  }, [enabled, ...deps]);

  useEffect(() => {
    if (enabled) {
      execute();
    }
  }, [execute]);

  return { data, isLoading, isError, error, refetch: execute };
}

export interface MutationResult<TData, TVariables> {
  mutate: (variables: TVariables) => Promise<TData>;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  reset: () => void;
}

export function useMutation<TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  options?: {
    onSuccess?: (data: TData, variables: TVariables) => void;
    onError?: (error: Error, variables: TVariables) => void;
  }
): MutationResult<TData, TVariables> {
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const mutate = useCallback(
    async (variables: TVariables): Promise<TData> => {
      setIsLoading(true);
      setIsError(false);
      setError(null);
      try {
        const result = await mutationFn(variables);
        options?.onSuccess?.(result, variables);
        return result;
      } catch (err: any) {
        const caughtError = err instanceof Error ? err : new Error(String(err));
        setIsError(true);
        setError(caughtError);
        options?.onError?.(caughtError, variables);
        throw caughtError;
      } finally {
        setIsLoading(false);
      }
    },
    [mutationFn, options]
  );

  const reset = useCallback(() => {
    setIsLoading(false);
    setIsError(false);
    setError(null);
  }, []);

  return { mutate, isLoading, isError, error, reset };
}
