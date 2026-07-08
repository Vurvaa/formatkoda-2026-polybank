import type { AccountResponseDto } from './account.ts';

export interface UserLoginRef {
  value: string;
}

export interface UserDetailsResponseDto {
  login: UserLoginRef;
  name: string;
  lastName: string;
  roles: string[];
  createdAt: string;
  blockedAt: string | null;
}

export interface UserInfoResponseDto extends UserDetailsResponseDto {
  accounts: AccountResponseDto[];
}

export interface StaffUserRegistrationDto {
  login: string;
  name: string;
  lastName: string;
  password: string;
  roleName: string;
}
