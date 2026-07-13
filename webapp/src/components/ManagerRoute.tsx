import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.tsx';

export function ManagerRoute() {
  const { isManager } = useAuth();

  if (!isManager) {
    return <Navigate to="/accounts" replace />;
  }

  return <Outlet />;
}
