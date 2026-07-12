import { PlusOutlined } from '@ant-design/icons';
import {
  App as AntdApp,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type TablePaginationConfig,
  type TableProps
} from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth.tsx';
import { UserAccountsModal } from '../components/UserAccountsModal.tsx';
import type { StaffUserRegistrationDto, UserDetailsResponseDto } from '../models/user.ts';
import { userService } from '../services/userService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';
import { formatDateTime } from '../utils/format.ts';

const roleOptions = [
  { label: 'MANAGER', value: 'MANAGER' },
  { label: 'SENIOR_MANAGER', value: 'SENIOR_MANAGER' }
];

export function UsersPage() {
  const { isManager, isSeniorManager } = useAuth();
  const { message } = AntdApp.useApp();

  const [users, setUsers] = useState<UserDetailsResponseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [actionLogin, setActionLogin] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [form] = Form.useForm<StaffUserRegistrationDto>();
  const [submitting, setSubmitting] = useState(false);
  const [accountsLogin, setAccountsLogin] = useState<string | null>(null);

  const loadUsers = useCallback(async () => {
    setLoading(true);

    try {
      const data = await userService.getAll(page, size);
      setUsers(data.items);
      setPage(data.page);
      setSize(data.size);
      setTotal(data.total);
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось загрузить пользователей'));
    } finally {
      setLoading(false);
    }
  }, [message, page, size]);

  useEffect(() => {
    loadUsers().then((r) => r);
  }, [loadUsers]);

  async function handleBlock(login: string) {
    setActionLogin(login);

    try {
      await userService.block(login);
      message.success('Пользователь заблокирован');
      await loadUsers();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось заблокировать пользователя'));
    } finally {
      setActionLogin(null);
    }
  }

  async function handleUnblock(login: string) {
    setActionLogin(login);

    try {
      await userService.unblock(login);
      message.success('Пользователь разблокирован');
      await loadUsers();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось разблокировать пользователя'));
    } finally {
      setActionLogin(null);
    }
  }

  async function handleRemoveRole(login: string, roleName: string) {
    setActionLogin(login);

    try {
      await userService.removeRole(login, roleName);
      message.success('Роль удалена');
      await loadUsers();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось удалить роль'));
    } finally {
      setActionLogin(null);
    }
  }

  async function handleCreateStaff(values: StaffUserRegistrationDto) {
    setSubmitting(true);

    try {
      await userService.createStaffUser(values);
      message.success('Сотрудник создан');
      setCreateOpen(false);
      form.resetFields();
      await loadUsers();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось создать сотрудника'));
    } finally {
      setSubmitting(false);
    }
  }

  const columns: TableProps<UserDetailsResponseDto>['columns'] = [
    {
      title: 'Логин',
      key: 'login',
      render: (_, record) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => setAccountsLogin(record.login.value)}>
          {record.login.value}
        </Button>
      )
    },
    { title: 'Имя', dataIndex: 'name' },
    { title: 'Фамилия', dataIndex: 'lastName' },
    {
      title: 'Роли',
      dataIndex: 'roles',
      render: (roles: string[], record) => (
        <Space wrap>
          {roles.map((role) => (
            <Tag
              key={role}
              closable={isSeniorManager}
              onClose={(e) => {
                e.preventDefault();
                handleRemoveRole(record.login.value, role).then((r) => r);
              }}
            >
              {role}
            </Tag>
          ))}
        </Space>
      )
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      render: (value: string) => formatDateTime(value)
    },
    {
      title: 'Статус',
      dataIndex: 'blockedAt',
      render: (value: string | null) => (value ? <Tag color="error">Заблокирован</Tag> : <Tag color="success">Активен</Tag>)
    },
    {
      title: '',
      key: 'actions',
      align: 'right',
      render: (_, record) =>
        isSeniorManager ? (
          record.blockedAt ? (
            <Button
              size="small"
              loading={actionLogin === record.login.value}
              onClick={() => handleUnblock(record.login.value)}
            >
              Разблокировать
            </Button>
          ) : (
            <Button
              size="small"
              danger
              loading={actionLogin === record.login.value}
              onClick={() => handleBlock(record.login.value)}
            >
              Заблокировать
            </Button>
          )
        ) : null
    }
  ];

  const pagination: TablePaginationConfig = {
    current: page + 1,
    pageSize: size,
    total,
    showSizeChanger: true,
    pageSizeOptions: [10, 20, 50],
    showTotal: (count) => `Всего: ${count}`,
    onChange: (nextPage, nextSize) => {
      setPage(nextPage - 1);
      setSize(nextSize);
    }
  };

  return (
    <Space direction="vertical" size="large" className="page-stack">
      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
        <Typography.Title level={2} style={{ margin: 0 }}>
          Пользователи
        </Typography.Title>

        {isSeniorManager && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            Добавить сотрудника
          </Button>
        )}
      </Space>

      <Card>
        <Table
          rowKey={(record) => record.login.value}
          columns={columns}
          dataSource={users}
          loading={loading}
          pagination={pagination}
          scroll={{ x: 900 }}
        />
      </Card>

      <Modal
        title="Новый сотрудник"
        open={createOpen}
        okText="Создать"
        cancelText="Отмена"
        confirmLoading={submitting}
        onCancel={() => setCreateOpen(false)}
        onOk={() => form.submit()}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={handleCreateStaff} preserve={false}>
          <Form.Item name="login" label="Логин" rules={[{ required: true, message: 'Введите логин' }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="name" label="Имя" rules={[{ required: true, message: 'Введите имя' }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="lastName"
            label="Фамилия"
            rules={[{ required: true, message: 'Введите фамилию' }]}
          >
            <Input />
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
          <Form.Item
            name="roleName"
            label="Роль"
            rules={[{ required: true, message: 'Выберите роль' }]}
          >
            <Select options={roleOptions} placeholder="Выберите роль" />
          </Form.Item>
        </Form>
      </Modal>

      <UserAccountsModal
        open={accountsLogin !== null}
        userLogin={accountsLogin}
        canManageAccounts={isManager}
        onClose={() => setAccountsLogin(null)}
      />
    </Space>
  );
}
