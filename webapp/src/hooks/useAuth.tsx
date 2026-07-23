import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren
} from 'react';
import type { UserRegistrationDto } from '../models/auth.ts';
import { authService } from '../services/authService.ts';
import { userService } from '../services/userService.ts';
import { authStorage } from '../utils/authStorage.ts';
import { extractLoginFromToken } from '../utils/jwt.ts';
import {exchangeCode, saveIdToken, startLogin, startLogout} from "../services/oauthService.ts";

interface AuthContextValue {
  token: string | null;
  login_: string | null;
  email: string | null;
  roles: string[];
  name: string | null;
  lastName: string | null;
  isAuthenticated: boolean;
  isManager: boolean;
  isSeniorManager: boolean;
  login: (returnTo?: string) => Promise<void>;
  completeLogin: (search: string) => Promise<void>;
  register: (payload: UserRegistrationDto) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: Readonly<PropsWithChildren>) {
  const [token, setToken] = useState<string | null>(() => authStorage.getToken());
  const [roles, setRoles] = useState<string[]>([]);
  const [name, setName] = useState<string | null>(null);
  const [lastName, setLastName] = useState<string | null>(null);
  const [email, setEmail] = useState<string | null>(null);
  const login_ = useMemo(() => (token ? extractLoginFromToken(token) : null), [token]);

  useEffect(() => {
    let cancelled = false;

    if (!login_) {
      setRoles([]);
      setName(null);
      setLastName(null);
      return;
    }

    userService
      .getInfo(login_)
      .then((info) => {
        if (!cancelled) {
          setRoles(info.roles);
          setEmail(info.email);
          setName(info.name);
          setLastName(info.lastName);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRoles([]);
          setEmail(null);
          setName(null);
          setLastName(null);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [login_]);

  const login = useCallback(async (returnTo = '/accounts') => {
    await startLogin(returnTo);
  }, []);

  const completeLogin = useCallback(async (search: string) => {
    const response = await exchangeCode(search);

    authStorage.setToken(response.access_token);
    saveIdToken(response.id_token);
    setToken(response.access_token);
  }, []);

  const register = useCallback(async (payload: UserRegistrationDto) => {
    const response = await authService.register(payload);
    authStorage.setToken(response.token);
    setToken(response.token);
  }, []);

  const logout = useCallback(() => {
    authStorage.clearToken();
    setToken(null);
    setRoles([]);
    setName(null);
    setLastName(null);

    startLogout();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      login_,
      email,
      roles,
      name,
      lastName,
      isAuthenticated: Boolean(token),
      isManager: roles.includes('MANAGER') || roles.includes('SENIOR_MANAGER'),
      isSeniorManager: roles.includes('SENIOR_MANAGER'),
      login,
      completeLogin,
      register,
      logout
    }),
    [login, completeLogin, login_, email, logout, register, roles, name, lastName, token]
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
