export type AccountStatus =
    | 'ACTIVE'
    | 'FROZEN'
    | 'BLOCKED'
    | 'CLOSED';

export type AccountType =
    | 'CURRENT'
    | 'FIXED_DEPOSIT'
    | 'SAVINGS'
    | 'CREDIT';

export interface AccountResponseDto {
  number: string;
  balance: number;
  type: AccountType;
  status: AccountStatus;
  createdAt: string;
}

export interface AccountOperationRequestDto {
  accountNumber: string;
  amount: number;
}

export interface TransferRequestDto {
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: number;
}
