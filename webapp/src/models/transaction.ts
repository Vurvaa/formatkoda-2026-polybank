export type TransactionStatus =
  | 'PENDING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELED'
  | 'REJECTED';

export type TransactionType =
  | 'TRANSFER'
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'PAYMENT'
  | 'REFUND'
  | 'INTEREST';

export type ReportFormat = 'CSV' | 'PDF'

export interface TransactionResponseDto {
  id: number;
  fromAccountNumber: string | null;
  toAccountNumber: string | null;
  amount: string;
  type: TransactionType;
  status: TransactionStatus;
  createdAt: string;
}
