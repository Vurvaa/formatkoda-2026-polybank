import { BankOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons';
import {
  Button,
  Dropdown,
  Layout,
  Menu,
  Space,
  Typography,
  type MenuProps
} from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.tsx';

const { Header, Content } = Layout;

export function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout, isManager } = useAuth();

  function getSelectedKey(): string {
    if (location.pathname.startsWith('/accounts')) {
      return '/accounts';
    }

    if (location.pathname.startsWith('/users')) {
      return '/users';
    }

    if (location.pathname.startsWith('/statistics')) {
      return '/statistics';
    }

    return '';
  }

  const navigationItems: MenuProps['items'] = [
    {
      key: '/accounts',
      label: 'Счета'
    },
    ...(isManager
        ? [
          {
            key: '/users',
            label: 'Пользователи'
          },
          {
            key: '/statistics',
            label: 'Статистика'
          }
        ]
        : [])
  ];

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
      onClick: logout
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

            <Menu
                mode="horizontal"
                items={navigationItems}
                selectedKeys={[getSelectedKey()]}
                onClick={({ key }) => navigate(key)}
            />
          </Space>

          <Dropdown
              menu={{ items: profileMenuItems }}
              trigger={['click', 'hover']}
              placement="bottomRight"
          >
            <Button
                type={location.pathname.startsWith('/profile') ? 'primary' : 'default'}
                icon={<UserOutlined />}
            >
              Профиль
            </Button>
          </Dropdown>
        </Header>

        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
  );
}