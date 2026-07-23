import {Button, Card, Space, Typography} from 'antd';
import {Navigate, useLocation, useNavigate} from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.tsx';

interface LocationState {
  from?: {
    pathname?: string;
  };
}

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const location = useLocation();
  const state = location.state as LocationState | null;
  const redirectTo = state?.from?.pathname ?? '/accounts';
  const navigate = useNavigate();

  if (isAuthenticated) {
    return <Navigate to="/accounts" replace />;
  }

  return (
      <div className="auth-page">
        <Card className="auth-card">
          <Typography.Title level={3} className="auth-title">
            Вход в личный кабинет
          </Typography.Title>
          <Space
              direction="vertical"
              size="middle"
              style={{ display: 'flex', width: '100%' }}
          >
            <Button
                type="primary"
                block
                size="large"
                onClick={() => void login(redirectTo)}
            >
              Войти
            </Button>

            <Button
                type="default"
                block
                size="large"
                onClick={() => navigate("/register")}
            >
              Регистрация
            </Button>
          </Space>
        </Card>
      </div>
  );
}