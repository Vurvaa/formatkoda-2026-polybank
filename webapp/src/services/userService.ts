import type { PageResponse } from '../models/page.ts';
import type {
  ChangedUserPasswordDto,
  StaffUserRegistrationDto,
  UserDetailsResponseDto,
  UserInfoResponseDto
} from '../models/user.ts';
import { api } from './api.ts';

export const userService = {
  async getAll(page: number, size: number): Promise<PageResponse<UserDetailsResponseDto>> {
    const response = await api.get<PageResponse<UserDetailsResponseDto>>('/user', {
      params: { page, size }
    });
    return response.data;
  },

  async getInfo(userLogin: string): Promise<UserInfoResponseDto> {
    const response = await api.get<UserInfoResponseDto>(`/user/${userLogin}/info`);
    return response.data;
  },

  async block(userLogin: string): Promise<UserDetailsResponseDto> {
    const response = await api.put<UserDetailsResponseDto>(`/user/${userLogin}/block`);
    return response.data;
  },

  async unblock(userLogin: string): Promise<UserDetailsResponseDto> {
    const response = await api.put<UserDetailsResponseDto>(`/user/${userLogin}/unblock`);
    return response.data;
  },

  async removeRole(userLogin: string, roleName: string): Promise<UserDetailsResponseDto> {
    const response = await api.delete<UserDetailsResponseDto>(`/user/${userLogin}/roles/${roleName}`);
    return response.data;
  },

  async addRole(userLogin: string, roleName: string): Promise<UserDetailsResponseDto> {
    const response = await api.put<UserDetailsResponseDto>(`/user/${userLogin}/roles/${roleName}`);
    return response.data;
  },

  async createStaffUser(payload: StaffUserRegistrationDto): Promise<UserDetailsResponseDto> {
    const response = await api.post<UserDetailsResponseDto>('/user', payload);
    return response.data;
  },

  async changePassword(payload: ChangedUserPasswordDto): Promise<void> {
    await api.put<void>('/user/password', payload);
  }
};
