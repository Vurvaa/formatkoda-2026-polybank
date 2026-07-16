import { App as AntdApp, Button, Card, Form, Input, Typography } from 'antd';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.tsx';
import type { UserRegistrationDto } from '../models/auth.ts';
import { getApiErrorMessage } from '../utils/errors.ts';

export function RegisterPage() {
  const { register, isAuthenticated } = useAuth();
  const { message } = AntdApp.useApp();
  const navigate = useNavigate();

  if (isAuthenticated) {
    return <Navigate to="/accounts" replace />;
  }

  async function handleSubmit(values: UserRegistrationDto) {
    try {
      await register(values);
      navigate('/accounts', { replace: true });
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось зарегистрироваться'));
    }
  }

  return (
    <div className="auth-page">
      <Card className="auth-card">
        <Typography.Title level={3} className="auth-title">
          Регистрация
        </Typography.Title>

        <Form layout="vertical" onFinish={handleSubmit} requiredMark={false}>
          <Form.Item name="login" label="Логин" rules={[{ required: true, message: 'Введите логин' }]}>
            <Input autoComplete="username" />
          </Form.Item>

          <Form.Item name="email" label="Email" rules={[{ required: true, message: 'Введите email' }]}>
            <Input autoComplete="email" />
          </Form.Item>

          <Form.Item name="name" label="Имя" rules={[{ required: true, message: 'Введите имя' }]}>
            <Input autoComplete="given-name" />
          </Form.Item>

          <Form.Item
            name="lastName"
            label="Фамилия"
            rules={[{ required: true, message: 'Введите фамилию' }]}
          >
            <Input autoComplete="family-name" />
          </Form.Item>

          <Form.Item
            name="password"
            label="Пароль"
            rules={[
              { required: true, message: 'Введите пароль' },
              { min: 8, message: 'Пароль должен быть не короче 8 символов' }
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>

          <Button type="primary" htmlType="submit" block size="large">
            Создать аккаунт
          </Button>
        </Form>

        <Typography.Paragraph className="auth-footer">
          Уже есть аккаунт? <Link to="/login">Войти</Link>
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
