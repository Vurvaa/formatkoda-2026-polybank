import { Card, Descriptions, Space, Tag, Typography } from 'antd';
import { useAuth } from '../hooks/useAuth.tsx';

export function ProfilePage() {
  const { login_, name, lastName, roles } = useAuth();

  return (
    <Space direction="vertical" size="large" className="page-stack">
      <Typography.Title level={2} style={{ margin: 0 }}>
        Профиль
      </Typography.Title>

      <Card>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Логин">{login_}</Descriptions.Item>
          <Descriptions.Item label="Имя">{name}</Descriptions.Item>
          <Descriptions.Item label="Фамилия">{lastName}</Descriptions.Item>
          <Descriptions.Item label="Роль">
            <Space wrap>
              {roles.map((role) => (
                <Tag key={role}>{role}</Tag>
              ))}
            </Space>
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </Space>
  );
}
