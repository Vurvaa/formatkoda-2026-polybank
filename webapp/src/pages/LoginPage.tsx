import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { App as AntdApp, Button, Card, Form, Input, Typography } from 'antd';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.tsx';
import type { LoginRequestDto } from '../models/auth.ts';
import { getApiErrorMessage } from '../utils/errors.ts';

interface LocationState {
  from?: { pathname?: string; };
}

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const { message } = AntdApp.useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as LocationState | null;
  const redirectTo = state?.from?.pathname ?? '/accounts';

  if (isAuthenticated) {
    return <Navigate to="/accounts" replace />;
  }

  async function handleSubmit(values: LoginRequestDto) {
    try {
      await login(values);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось войти'));
    }
  }

  return (
    <div className="auth-page">
      <Card className="auth-card">
        <Typography.Title level={3} className="auth-title">
          Вход в личный кабинет
        </Typography.Title>

        <Form layout="vertical" onFinish={handleSubmit} requiredMark={false}>
          <Form.Item name="login" label="Логин" rules={[{ required: true, message: 'Введите логин' }]}>
            <Input prefix={<UserOutlined />} autoComplete="username" placeholder="login" />
          </Form.Item>

          <Form.Item
            name="password"
            label="Пароль"
            rules={[{ required: true, message: 'Введите пароль' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              autoComplete="current-password"
              placeholder="password"
            />
          </Form.Item>

          <Button type="primary" htmlType="submit" block size="large">
            Войти
          </Button>
        </Form>

        <Typography.Paragraph className="auth-footer">
          Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
