import type {
  AccountOperationRequestDto,
  AccountResponseDto,
  TransferRequestDto
} from '../models/account.ts';
import type { PageResponse } from '../models/page.ts';
import type { TransactionResponseDto } from '../models/transaction.ts';
import { api } from './api.ts';

export const accountService = {
  async getAll(): Promise<AccountResponseDto[]> {
    const response = await api.get<AccountResponseDto[]>('/account');
    return response.data;
  },

  async get(accountNumber: string): Promise<AccountResponseDto> {
    const response = await api.get<AccountResponseDto>(`/account/${accountNumber}`);
    return response.data;
  },

  async topUp(payload: AccountOperationRequestDto): Promise<TransactionResponseDto> {
    const response = await api.post<TransactionResponseDto>('/transaction/top-up', payload);
    return response.data;
  },

  async withdraw(payload: AccountOperationRequestDto): Promise<TransactionResponseDto> {
    const response = await api.post<TransactionResponseDto>('/transaction/withdraw', payload);
    return response.data;
  },

  async transfer(payload: TransferRequestDto): Promise<TransactionResponseDto> {
    const response = await api.post<TransactionResponseDto>('/transaction/transfer', payload);
    return response.data;
  },

  async getTransactions(accountNumber: string, page: number, size: number): Promise<PageResponse<TransactionResponseDto>> {
    const response = await api.get<PageResponse<TransactionResponseDto>>(
      `/transaction/${accountNumber}`, { params: { page, size } });
    return response.data;
  }
};
