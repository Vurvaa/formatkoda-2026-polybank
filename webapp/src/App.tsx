import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout.tsx';
import { ProtectedRoute } from './components/ProtectedRoute.tsx';
import { AccountDetailsPage } from './pages/AccountDetailsPage.tsx';
import { AccountsPage } from './pages/AccountsPage.tsx';
import { LoginPage } from './pages/LoginPage.tsx';
import { NotFoundPage } from './pages/NotFoundPage.tsx';
import { RegisterPage } from './pages/RegisterPage.tsx';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/accounts" replace />} />
          <Route path="/accounts" element={<AccountsPage />} />
          <Route path="/accounts/:accountNumber" element={<AccountDetailsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
