import type { AuthUserDto, LoginRequestDto, UserRegistrationDto } from '../models/auth.ts';
import { api } from './api.ts';

export const authService = {
  async login(payload: LoginRequestDto): Promise<AuthUserDto> {
    const response = await api.post<AuthUserDto>('/user/sign-in', payload);
    return response.data;
  },

  async register(payload: UserRegistrationDto): Promise<AuthUserDto> {
    const response = await api.post<AuthUserDto>('/user/sign-up', payload);
    return response.data;
  }
};
