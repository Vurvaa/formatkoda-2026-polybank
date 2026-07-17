import type { AccountStatus, AccountType } from './account.ts';

export interface UserLoginRef {
  value: string;
}

export interface UserDetailsResponseDto {
  login: UserLoginRef;
  email: string;
  name: string;
  lastName: string;
  roles: string[];
  createdAt: string;
  blockedAt: string | null;
}

export interface AccountNumberRef {
  value: string;
}

export interface AccountInfoDto {
  number: AccountNumberRef;
  type: AccountType;
  status: AccountStatus;
  createdAt: string;
}

export interface UserInfoResponseDto extends UserDetailsResponseDto {
  accounts: AccountInfoDto[];
}

export interface StaffUserRegistrationDto {
  login: string;
  name: string;
  lastName: string;
  password: string;
  roleName: string;
}

export interface ChangedUserPasswordDto {
  login: string;
  oldPassword: string;
  newPassword: string;
}