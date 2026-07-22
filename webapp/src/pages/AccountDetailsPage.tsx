import {
  ArrowLeftOutlined,
  DownloadOutlined,
  ExportOutlined,
  LockOutlined,
  SendOutlined,
  StopOutlined,
  UnlockOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { App as AntdApp, Button, Card, Descriptions, Popconfirm, Space, Tag, Typography } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { OperationModal, type AccountOperation } from '../components/OperationModal.tsx';
import { TransactionsTable } from '../components/TransactionsTable.tsx';
import { useAuth } from '../hooks/useAuth.tsx';
import type { AccountResponseDto } from '../models/account.ts';
import type { TransactionResponseDto } from '../models/transaction.ts';
import { accountService } from '../services/accountService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';
import { formatDateTime, formatMoney } from '../utils/format.ts';
import {ReportModal} from "../components/ReportModal.tsx";

export function AccountDetailsPage() {
  const { accountNumber } = useParams<{ accountNumber: string }>();
  const [account, setAccount] = useState<AccountResponseDto | null>(null);
  const [transactions, setTransactions] = useState<TransactionResponseDto[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [accountLoading, setAccountLoading] = useState(true);
  const [transactionsLoading, setTransactionsLoading] = useState(true);
  const [operation, setOperation] = useState<AccountOperation | null>(null);
  const [report, setReport] = useState<boolean>(false);
  const [actionLoading, setActionLoading] = useState(false);
  const { message } = AntdApp.useApp();
  const { isManager } = useAuth();

  const loadAccount = useCallback(async () => {
    if (!accountNumber) {
      return;
    }

    setAccountLoading(true);

    try {
      const data = await accountService.get(accountNumber);
      setAccount(data);
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось загрузить счет'));
    } finally {
      setAccountLoading(false);
    }
  }, [accountNumber, message]);

  const loadTransactions = useCallback(async () => {
    if (!accountNumber) {
      return;
    }

    setTransactionsLoading(true);

    try {
      const data = await accountService.getTransactions(accountNumber, page, size);
      setTransactions(data.items);
      setPage(data.page);
      setSize(data.size);
      setTotal(data.total);
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось загрузить транзакции'));
    } finally {
      setTransactionsLoading(false);
    }
  }, [accountNumber, message, page, size]);

  useEffect(() => {loadAccount().then(r => r)}, [loadAccount]);
  useEffect(() => {loadTransactions().then(r => r);}, [loadTransactions]);

  if (!accountNumber) {
    return <Navigate to="/accounts" replace />;
  }

  function handleOperationSuccess() {
    loadAccount().then(r => r);
    setPage(0);
    loadTransactions().then(r => r);
  }

  function handlePageChange(nextPage: number, nextSize: number) {
    setPage(nextPage);
    setSize(nextSize);
  }

  async function runAccountAction(action: () => Promise<AccountResponseDto>, successMessage: string) {
    setActionLoading(true);

    try {
      await action();
      message.success(successMessage);
      await loadAccount();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось выполнить действие'));
    } finally {
      setActionLoading(false);
    }
  }

  async function handleCancelTransaction(transactionId: number) {
    try {
      await accountService.cancelTransaction(transactionId);
      message.success('Транзакция отменена');
      loadAccount().then(r => r);
      loadTransactions().then(r => r);
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось отменить транзакцию'));
    }
  }

  return (
    <Space direction="vertical" size="large" className="page-stack">
      <Space direction="vertical" size="small">
        <Link to="/accounts">
          <ArrowLeftOutlined /> Назад к счетам
        </Link>

        <Typography.Title level={2}>Счет {accountNumber}</Typography.Title>
      </Space>

      <Card loading={accountLoading}>
        <Descriptions column={2} bordered>
          <Descriptions.Item label="Номер счета">
            <Typography.Text>{accountNumber}</Typography.Text>
          </Descriptions.Item>
          <Descriptions.Item label="Тип">
            {account ? account.type : '...'}
          </Descriptions.Item>
          <Descriptions.Item label="Баланс">
            {account ? formatMoney(account.balance) : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Создан">
            {formatDateTime(account?.createdAt)}
          </Descriptions.Item>
          <Descriptions.Item label="Статус">
            {account ? <Tag>{account.status}</Tag> : '-'}
          </Descriptions.Item>
        </Descriptions>

        <Space wrap className="account-actions">
          <Button type="primary" icon={<DownloadOutlined />} onClick={() => setOperation('topUp')}>
            Начислить деньги
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => setOperation('withdraw')}>
            Снять деньги
          </Button>
          <Button icon={<SendOutlined />} onClick={() => setOperation('transfer')}>
            Перевести
          </Button>

          {account?.status === 'ACTIVE' && (
            <Button
              icon={<LockOutlined />}
              loading={actionLoading}
              onClick={() => runAccountAction(() => accountService.freeze(accountNumber), 'Счет заморожен')}
            >
              Заморозить
            </Button>
          )}

          {account?.status === 'FROZEN' && (
            <Button
              icon={<UnlockOutlined />}
              loading={actionLoading}
              onClick={() => runAccountAction(() => accountService.unfreeze(accountNumber), 'Счет разморожен')}
            >
              Разморозить
            </Button>
          )}

          {account && account.status !== 'CLOSED' && (
            <Popconfirm
              title="Закрыть счет?"
              description="Действие необратимо"
              okText="Закрыть"
              cancelText="Отмена"
              onConfirm={() =>
                runAccountAction(() => accountService.close(accountNumber), 'Счет закрыт')
              }
            >
              <Button danger icon={<StopOutlined />} loading={actionLoading}>
                Закрыть счет
              </Button>

            </Popconfirm>
          )}

          <Button
              icon={<ExportOutlined />}
              loading={actionLoading}
              onClick={() => setReport(true)}
          >
            Выписка по счету
          </Button>

          {isManager && account?.status !== 'BLOCKED' && (
            <Button
              danger
              loading={actionLoading}
              onClick={() => runAccountAction(() => accountService.block(accountNumber), 'Счет заблокирован')}
            >
              Заблокировать (менеджер)
            </Button>
          )}

          {isManager && account?.status === 'BLOCKED' && (
            <Button
              loading={actionLoading}
              onClick={() => runAccountAction(() => accountService.unblock(accountNumber), 'Счет разблокирован')}
            >
              Разблокировать (менеджер)
            </Button>
          )}
        </Space>
      </Card>

      <Card title="Последние транзакции">
        <TransactionsTable
          transactions={transactions}
          loading={transactionsLoading}
          page={page}
          size={size}
          total={total}
          onPageChange={handlePageChange}
          canCancel={isManager}
          onCancel={handleCancelTransaction}
        />
      </Card>

      <OperationModal
        accountNumber={accountNumber}
        operation={operation}
        open={Boolean(operation)}
        onCancel={() => setOperation(null)}
        onSuccess={handleOperationSuccess}
      />

      <ReportModal
        accountNumber={accountNumber}
        open={report}
        onCancel={() => setReport(false)}
      />
    </Space>
  );
}
