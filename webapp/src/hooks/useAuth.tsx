import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren
} from 'react';
import type { LoginRequestDto, UserRegistrationDto } from '../models/auth.ts';
import { authService } from '../services/authService.ts';
import { authStorage } from '../utils/authStorage.ts';

interface AuthContextValue {
  token: string | null;
  isAuthenticated: boolean;
  login: (payload: LoginRequestDto) => Promise<void>;
  register: (payload: UserRegistrationDto) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: Readonly<PropsWithChildren>) {
  const [token, setToken] = useState<string | null>(() => authStorage.getToken());

  const login = useCallback(async (payload: LoginRequestDto) => {
    const response = await authService.login(payload);
    authStorage.setToken(response.token);
    setToken(response.token);
  }, []);

  const register = useCallback(async (payload: UserRegistrationDto) => {
    const response = await authService.register(payload);
    authStorage.setToken(response.token);
    setToken(response.token);
  }, []);

  const logout = useCallback(() => {
    authStorage.clearToken();
    setToken(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      isAuthenticated: Boolean(token),
      login,
      register,
      logout
    }),
    [login, logout, register, token]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth должен использоваться внутри AuthProvider');
  }

  return context;
}
