import { App as AntdApp, Button, Card, Space, Table, Typography, type TableProps } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { AccountResponseDto } from '../models/account.ts';
import { accountService } from '../services/accountService.ts';
import { formatDateTime, formatMoney } from '../utils/format.ts';
import { getApiErrorMessage } from '../utils/errors.ts';

export function AccountsPage() {
  const [accounts, setAccounts] = useState<AccountResponseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const { message } = AntdApp.useApp();
  const navigate = useNavigate();

  useEffect(() => {loadAccounts().then(r => r)}, []);

  async function loadAccounts() {
    setLoading(true);

    try {
      const data = await accountService.getAll();
      setAccounts(data);
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось загрузить счета'));
    } finally {
      setLoading(false);
    }
  }

  const columns: TableProps<AccountResponseDto>['columns'] = [
    {
      title: 'Счет',
      dataIndex: 'number',
      render: (value: string) => value
    },
    {
      title: 'Тип',
      dataIndex: 'type',
      render: (value: string) => value
    },
    {
      title: 'Баланс',
      dataIndex: 'balance',
      align: 'right',
      render: (value: string) => formatMoney(value)
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
      render: (_, record) => (
        <Button type="link" onClick={() => navigate(`/accounts/${record.number}`)}>
          Открыть
        </Button>
      )
    }
  ];

  return (
    <Space direction="vertical" size="large" className="page-stack">
      <Typography.Title level={2}>Мои счета</Typography.Title>

      <Card>
        <Table
          key="number"
          rowKey="number"
          columns={columns}
          dataSource={accounts}
          loading={loading}
          pagination={false}
          scroll={{ x: 760 }}
        />
      </Card>
    </Space>
  );
}
