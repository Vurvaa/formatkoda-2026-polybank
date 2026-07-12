import { ArrowLeftOutlined } from '@ant-design/icons';
import { App as AntdApp, Button, Card, Descriptions, Space, Table, Tag, Typography, type TableProps } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { AccountResponseDto } from '../models/account.ts';
import type { UserInfoResponseDto } from '../models/user.ts';
import { userService } from '../services/userService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';
import { formatDateTime, formatMoney } from '../utils/format.ts';

export function UserDetailsPage() {
  const { userLogin } = useParams<{ userLogin: string }>();
  const [user, setUser] = useState<UserInfoResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const { message } = AntdApp.useApp();

  const loadUser = useCallback(async () => {
    if (!userLogin) {
      return;
    }

    setLoading(true);

    try {
      const data = await userService.getInfo(userLogin);
      setUser(data);
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось загрузить информацию о пользователе'));
    } finally {
      setLoading(false);
    }
  }, [message, userLogin]);

  useEffect(() => {
    loadUser().then((r) => r);
  }, [loadUser]);

  const columns: TableProps<AccountResponseDto>['columns'] = [
    { title: 'Счет', dataIndex: 'number' },
    { title: 'Тип', dataIndex: 'type' },
    {
      title: 'Баланс',
      dataIndex: 'balance',
      align: 'right',
      render: (value: string) => formatMoney(value)
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      render: (value: string) => <Tag>{value}</Tag>
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      render: (value?: string) => formatDateTime(value)
    }
  ];

  return (
    <Space direction="vertical" size="large" className="page-stack">
      <Space align="center">
        <Link to="/users">
          <Button icon={<ArrowLeftOutlined />}>Назад</Button>
        </Link>
        <Typography.Title level={2} style={{ margin: 0 }}>
          Пользователь {userLogin}
        </Typography.Title>
      </Space>

      <Card loading={loading}>
        {user && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="Логин">{user.login.value}</Descriptions.Item>
            <Descriptions.Item label="Имя">{user.name}</Descriptions.Item>
            <Descriptions.Item label="Фамилия">{user.lastName}</Descriptions.Item>
            <Descriptions.Item label="Роли">
              <Space wrap>
                {user.roles.map((role) => (
                  <Tag key={role}>{role}</Tag>
                ))}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Создан">{formatDateTime(user.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="Статус">
              {user.blockedAt ? <Tag color="error">Заблокирован</Tag> : <Tag color="success">Активен</Tag>}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Card>

      <Card title="Счета пользователя">
        <Table
          rowKey="number"
          columns={columns}
          dataSource={user?.accounts ?? []}
          loading={loading}
          pagination={false}
          scroll={{ x: 760 }}
        />
      </Card>
    </Space>
  );
}
