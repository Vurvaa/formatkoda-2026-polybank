export interface AuthUserDto {
  token: string;
}

export interface LoginRequestDto {
  login: string;
  password: string;
}

export interface UserRegistrationDto {
  login: string;
  email: string;
  name: string;
  lastName: string;
  password: string;
}
