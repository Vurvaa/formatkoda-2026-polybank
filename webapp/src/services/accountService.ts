import type {
  AccountOperationRequestDto,
  AccountResponseDto,
  CreateAccountRequestDto,
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

  async create(payload: CreateAccountRequestDto): Promise<AccountResponseDto> {
    const response = await api.post<AccountResponseDto>('/account', payload);
    return response.data;
  },

  async close(accountNumber: string): Promise<AccountResponseDto> {
    const response = await api.put<AccountResponseDto>(`/account/${accountNumber}/close`);
    return response.data;
  },

  async freeze(accountNumber: string): Promise<AccountResponseDto> {
    const response = await api.put<AccountResponseDto>(`/account/${accountNumber}/freeze`);
    return response.data;
  },

  async unfreeze(accountNumber: string): Promise<AccountResponseDto> {
    const response = await api.put<AccountResponseDto>(`/account/${accountNumber}/unfreeze`);
    return response.data;
  },

  async block(accountNumber: string): Promise<AccountResponseDto> {
    const response = await api.put<AccountResponseDto>(`/account/${accountNumber}/block`);
    return response.data;
  },

  async unblock(accountNumber: string): Promise<AccountResponseDto> {
    const response = await api.put<AccountResponseDto>(`/account/${accountNumber}/unblock`);
    return response.data;
  },

  async cancelTransaction(transactionId: number): Promise<TransactionResponseDto> {
    const response = await api.put<TransactionResponseDto>(`/transaction/${transactionId}/cancel`);
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