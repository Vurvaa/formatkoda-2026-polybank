import { ArrowLeftOutlined, PlusOutlined } from '@ant-design/icons';
import {
  App as AntdApp,
  Button,
  Card,
  Descriptions,
  Form,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TableProps
} from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.tsx';
import type { AccountInfoDto, UserInfoResponseDto } from '../models/user.ts';
import { accountService } from '../services/accountService.ts';
import { userService } from '../services/userService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';
import { formatDateTime } from '../utils/format.ts';

const roleOptions = [
  { label: 'MANAGER', value: 'MANAGER' },
  { label: 'SENIOR_MANAGER', value: 'SENIOR_MANAGER' }
];

export function UserDetailsPage() {
  const { userLogin } = useParams<{ userLogin: string }>();
  const { isManager, isSeniorManager } = useAuth();
  const [user, setUser] = useState<UserInfoResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [userActionLoading, setUserActionLoading] = useState(false);
  const [accountActionNumber, setAccountActionNumber] = useState<string | null>(null);
  const [addRoleOpen, setAddRoleOpen] = useState(false);
  const [addRoleForm] = Form.useForm<{ roleName: string }>();
  const [addRoleSubmitting, setAddRoleSubmitting] = useState(false);
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

  async function handleBlockUser() {
    if (!userLogin) {
      return;
    }

    setUserActionLoading(true);

    try {
      await userService.block(userLogin);
      message.success('Пользователь заблокирован');
      await loadUser();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось заблокировать пользователя'));
    } finally {
      setUserActionLoading(false);
    }
  }

  async function handleUnblockUser() {
    if (!userLogin) {
      return;
    }

    setUserActionLoading(true);

    try {
      await userService.unblock(userLogin);
      message.success('Пользователь разблокирован');
      await loadUser();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось разблокировать пользователя'));
    } finally {
      setUserActionLoading(false);
    }
  }

  async function handleRemoveRole(roleName: string) {
    if (!userLogin) {
      return;
    }

    setUserActionLoading(true);

    try {
      await userService.removeRole(userLogin, roleName);
      message.success('Роль удалена');
      await loadUser();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось удалить роль'));
    } finally {
      setUserActionLoading(false);
    }
  }

  async function handleAddRole(values: { roleName: string }) {
    if (!userLogin) {
      return;
    }

    setAddRoleSubmitting(true);

    try {
      await userService.addRole(userLogin, values.roleName);
      message.success('Роль добавлена');
      setAddRoleOpen(false);
      addRoleForm.resetFields();
      await loadUser();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось добавить роль'));
    } finally {
      setAddRoleSubmitting(false);
    }
  }

  async function handleBlockAccount(accountNumber: string) {
    setAccountActionNumber(accountNumber);

    try {
      await accountService.block(accountNumber);
      message.success('Счет заблокирован');
      await loadUser();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось заблокировать счет'));
    } finally {
      setAccountActionNumber(null);
    }
  }

  async function handleUnblockAccount(accountNumber: string) {
    setAccountActionNumber(accountNumber);

    try {
      await accountService.unblock(accountNumber);
      message.success('Счет разблокирован');
      await loadUser();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось разблокировать счет'));
    } finally {
      setAccountActionNumber(null);
    }
  }

  const columns: TableProps<AccountInfoDto>['columns'] = [
    {
      title: 'Счет',
      key: 'number',
      render: (_, record) => record.number.value
    },
    { title: 'Тип', dataIndex: 'type' },
    {
      title: 'Статус',
      dataIndex: 'status',
      render: (value: string) => <Tag>{value}</Tag>
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      render: (value?: string) => formatDateTime(value)
    },
    {
      title: '',
      key: 'actions',
      align: 'right',
      render: (_, record) => {
        if (!isManager) {
          return null;
        }

        if (record.status === 'BLOCKED') {
          return (
              <Button
                  size="small"
                  loading={accountActionNumber === record.number.value}
                  onClick={() => handleUnblockAccount(record.number.value)}
              >
                Разблокировать
              </Button>
          );
        }

        return (
            <Button
                size="small"
                danger
                loading={accountActionNumber === record.number.value}
                onClick={() => handleBlockAccount(record.number.value)}
            >
              Заблокировать
            </Button>
        );
      }
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
          <>
            <Descriptions column={1} bordered>
              <Descriptions.Item label="Логин">{user.login.value}</Descriptions.Item>
              <Descriptions.Item label="Имя">{user.name}</Descriptions.Item>
              <Descriptions.Item label="Фамилия">{user.lastName}</Descriptions.Item>
              <Descriptions.Item label="Роли">
                <Space wrap>
                  {user.roles.map((role) => (
                    <Tag
                      key={role}
                      closable={isSeniorManager}
                      onClose={(e) => {
                        e.preventDefault();
                        handleRemoveRole(role).then((r) => r);
                      }}
                    >
                      {role}
                    </Tag>
                  ))}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Создан">{formatDateTime(user.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="Статус">
                {user.blockedAt ? <Tag color="error">Заблокирован</Tag> : <Tag color="success">Активен</Tag>}
              </Descriptions.Item>
            </Descriptions>

            {isSeniorManager && (
              <Space wrap style={{ marginTop: 16 }}>
                <Button icon={<PlusOutlined />} onClick={() => setAddRoleOpen(true)}>
                  Добавить роль
                </Button>

                {user.blockedAt ? (
                  <Button loading={userActionLoading} onClick={handleUnblockUser}>
                    Разблокировать пользователя
                  </Button>
                ) : (
                  <Button danger loading={userActionLoading} onClick={handleBlockUser}>
                    Заблокировать пользователя
                  </Button>
                )}
              </Space>
            )}
          </>
        )}
      </Card>

      <Card title="Счета пользователя">
        <Table
          rowKey={(record) => record.number.value}
          columns={columns}
          dataSource={user?.accounts ?? []}
          loading={loading}
          pagination={false}
          scroll={{ x: 760 }}
        />
      </Card>

      <Modal
        title={`Добавить роль пользователю ${userLogin}`}
        open={addRoleOpen}
        okText="Добавить"
        cancelText="Отмена"
        confirmLoading={addRoleSubmitting}
        onCancel={() => setAddRoleOpen(false)}
        onOk={() => addRoleForm.submit()}
        destroyOnHidden
      >
        <Form form={addRoleForm} layout="vertical" onFinish={handleAddRole} preserve={false}>
          <Form.Item
            name="roleName"
            label="Роль"
            rules={[{ required: true, message: 'Выберите роль' }]}
          >
            <Select options={roleOptions} placeholder="Выберите роль" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
