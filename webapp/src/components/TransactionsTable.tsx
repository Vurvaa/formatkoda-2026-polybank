import { Tag, Table, Typography, type TablePaginationConfig, type TableProps } from 'antd';
import type {TransactionResponseDto, TransactionStatus, TransactionType} from '../models/transaction.ts';
import { formatDateTime, formatMoney } from '../utils/format.ts';

interface TransactionsTableProps {
  transactions: TransactionResponseDto[];
  loading: boolean;
  page: number;
  size: number;
  total: number;
  onPageChange: (page: number, size: number) => void;
}

const statusColor: Record<TransactionStatus, string> = {
  PENDING: 'processing',
  COMPLETED: 'success',
  FAILED: 'error',
  CANCELED: 'default',
  REJECTED: 'warning'
};

const typeLabel: Record<TransactionType, string> = {
  TRANSFER: 'Перевод',
  DEPOSIT: 'Пополнение',
  WITHDRAWAL: 'Снятие',
  PAYMENT: 'Платеж',
  REFUND: 'Возврат',
  INTEREST: 'Проценты'
};

export function TransactionsTable({
  transactions,
  loading,
  page,
  size,
  total,
  onPageChange
}: Readonly<TransactionsTableProps>) {
  const columns: TableProps<TransactionResponseDto>['columns'] = [
    {
      title: 'Дата',
      dataIndex: 'createdAt',
      render: (value: string) => formatDateTime(value)
    },
    {
      title: 'Тип',
      dataIndex: 'type',
      render: (value: TransactionType) => typeLabel[value] ?? value
    },
    {
      title: 'Откуда',
      dataIndex: 'fromAccountNumber',
      render: (value: string | null) => (
        <Typography.Text>{value || '-'}</Typography.Text>
      )
    },
    {
      title: 'Куда',
      dataIndex: 'toAccountNumber',
      render: (value: string | null) => (
        <Typography.Text>{value || '-'}</Typography.Text>
      )
    },
    {
      title: 'Сумма',
      dataIndex: 'amount',
      align: 'right',
      render: (value: string) => formatMoney(value)
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      render: (value: TransactionStatus) => (
        <Tag color={statusColor[value]}>{value}</Tag>
      )
    }
  ];

  const pagination: TablePaginationConfig = {
    current: page + 1,
    pageSize: size,
    total,
    showSizeChanger: true,
    pageSizeOptions: [10, 20, 50],
    showTotal: (count) => `Всего: ${count}`,
    onChange: (nextPage, nextSize) => onPageChange(nextPage - 1, nextSize)
  };

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={transactions}
      loading={loading}
      pagination={pagination}
      scroll={{ x: 900 }}
    />
  );
}
