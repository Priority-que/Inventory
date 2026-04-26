export interface LoginParams {
  username: string
  password: string
}

export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  name: string
  roleCodes: string[]
}

export interface CurrentUser {
  id: number
  username: string
  name: string
  dept?: string
  status?: string
  roleCodes: string[]
}
