export interface User {
  id: number;
  username: string;
  email: string;
  role?: string;
  activeRole?: string;
  student?: boolean;
  infoproductor?: boolean;
  profilePicture?: string;
  firstName?: string;
  lastName?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface GoogleLoginRequest {
  token: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  role: string;
  activeRole?: string;
  active?: boolean;
  student?: boolean;
  infoproductor?: boolean;
}
