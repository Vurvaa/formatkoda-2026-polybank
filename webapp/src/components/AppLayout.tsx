import { BankOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Dropdown, Layout, Space, Typography, type MenuProps } from 'antd';
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

  const profileMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      label: 'Профиль',
      icon: <UserOutlined />,
      onClick: () => navigate('/profile')
    },
    {
      key: 'logout',
      label: <span style={{ color: '#ff4d4f' }}>Выйти</span>,
      icon: <LogoutOutlined style={{ color: '#ff4d4f' }} />,
      onClick: handleLogout
    }
  ];

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
          {isManager && <Link to="/statistics">Статистика</Link>}
        </Space>

        <Dropdown menu={{ items: profileMenuItems }} trigger={['click', 'hover']} placement="bottomRight">
          <Button icon={<UserOutlined />}>Профиль</Button>
        </Dropdown>
      </Header>

      <Content className="app-content">
        <Outlet />
      </Content>
    </Layout>
  );
}
