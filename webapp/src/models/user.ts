import type { AccountStatus, AccountType } from './account.ts';

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

export interface AccountInfoDto {
  number: string;
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
