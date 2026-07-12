import { App as AntdApp, Modal, Table, Tag, Button, type TableProps } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import type { AccountInfoDto } from '../models/user.ts';
import { userService } from '../services/userService.ts';
import { accountService } from '../services/accountService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';
import { formatDateTime } from '../utils/format.ts';

interface UserAccountsModalProps {
  open: boolean;
  userLogin: string | null;
  canManageAccounts: boolean;
  onClose: () => void;
}

const statusColors: Record<string, string> = {
  ACTIVE: 'success',
  FROZEN: 'processing',
  BLOCKED: 'error',
  CLOSED: 'default'
};

export function UserAccountsModal({ open, userLogin, canManageAccounts, onClose }: Readonly<UserAccountsModalProps>) {
  const { message } = AntdApp.useApp();

  const [accounts, setAccounts] = useState<AccountInfoDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionNumber, setActionNumber] = useState<string | null>(null);

  const loadAccounts = useCallback(async () => {
    if (!userLogin) {
      return;
    }

    setLoading(true);

    try {
      const info = await userService.getInfo(userLogin);
      setAccounts(info.accounts);
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось загрузить счета пользователя'));
    } finally {
      setLoading(false);
    }
  }, [message, userLogin]);

  useEffect(() => {
    if (open) {
      loadAccounts().then((r) => r);
    } else {
      setAccounts([]);
    }
  }, [open, loadAccounts]);

  async function handleBlock(accountNumber: string) {
    setActionNumber(accountNumber);

    try {
      await accountService.block(accountNumber);
      message.success('Счет заблокирован');
      await loadAccounts();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось заблокировать счет'));
    } finally {
      setActionNumber(null);
    }
  }

  async function handleUnblock(accountNumber: string) {
    setActionNumber(accountNumber);

    try {
      await accountService.unblock(accountNumber);
      message.success('Счет разблокирован');
      await loadAccounts();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось разблокировать счет'));
    } finally {
      setActionNumber(null);
    }
  }

  const columns: TableProps<AccountInfoDto>['columns'] = [
    {
      title: 'Счет',
      dataIndex: 'number'
    },
    {
      title: 'Тип',
      dataIndex: 'type'
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      render: (value: string) => <Tag color={statusColors[value] ?? 'default'}>{value}</Tag>
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      render: (value: string) => formatDateTime(value)
    },
    {
      title: '',
      key: 'actions',
      align: 'right',
      render: (_, record) => {
        if (!canManageAccounts || record.status === 'CLOSED') {
          return null;
        }

        return record.status === 'BLOCKED' ? (
          <Button
            size="small"
            loading={actionNumber === record.number}
            onClick={() => handleUnblock(record.number)}
          >
            Разблокировать
          </Button>
        ) : (
          <Button
            size="small"
            danger
            loading={actionNumber === record.number}
            onClick={() => handleBlock(record.number)}
          >
            Заблокировать
          </Button>
        );
      }
    }
  ];

  return (
    <Modal
      title={userLogin ? `Счета пользователя: ${userLogin}` : 'Счета пользователя'}
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnHidden
      width={720}
    >
      <Table
        rowKey="number"
        columns={columns}
        dataSource={accounts}
        loading={loading}
        pagination={false}
        locale={{ emptyText: 'У пользователя нет счетов' }}
      />
    </Modal>
  );
}
