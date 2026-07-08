import { BankOutlined, LogoutOutlined } from '@ant-design/icons';
import { Button, Layout, Space, Typography } from 'antd';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.tsx';

const { Header, Content } = Layout;

export function AppLayout() {
  const navigate = useNavigate();
  const { logout, isManager } = useAuth();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <Layout className="app-layout">
      <Header className="app-header">
        <Space align="center" size="middle">
          <BankOutlined className="app-logo" />
          <Typography.Title level={4} className="app-title">
            POLYBANK
          </Typography.Title>

          <Link to="/accounts">
            <Button type="primary">Счета</Button>
          </Link>
          {isManager && <Link to="/users">Пользователи</Link>}
        </Space>

        <Button icon={<LogoutOutlined />} onClick={handleLogout}>
          Выйти
        </Button>
      </Header>

      <Content className="app-content">
        <Outlet />
      </Content>
    </Layout>
  );
}
