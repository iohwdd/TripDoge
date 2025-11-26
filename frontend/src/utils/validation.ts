// 表单验证工具函数

/**
 * 验证邮箱格式
 */
export const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

/**
 * 验证密码强度
 * 要求：至少8位，包含大小写字母和数字
 */
export const validatePassword = (password: string): { valid: boolean; message?: string } => {
  if (password.length < 8) {
    return { valid: false, message: '密码长度至少8位' }
  }
  if (!/[a-z]/.test(password)) {
    return { valid: false, message: '密码必须包含小写字母' }
  }
  if (!/[A-Z]/.test(password)) {
    return { valid: false, message: '密码必须包含大写字母' }
  }
  if (!/[0-9]/.test(password)) {
    return { valid: false, message: '密码必须包含数字' }
  }
  return { valid: true }
}

/**
 * 验证昵称
 */
export const validateNickname = (nickname: string): { valid: boolean; message?: string } => {
  if (!nickname || nickname.trim().length === 0) {
    return { valid: false, message: '昵称不能为空' }
  }
  if (nickname.length < 2) {
    return { valid: false, message: '昵称长度至少2位' }
  }
  if (nickname.length > 20) {
    return { valid: false, message: '昵称长度不能超过20位' }
  }
  return { valid: true }
}

/**
 * 验证验证码
 */
export const validateCode = (code: string): { valid: boolean; message?: string } => {
  if (!code || code.trim().length === 0) {
    return { valid: false, message: '验证码不能为空' }
  }
  if (code.length !== 6) {
    return { valid: false, message: '验证码必须为6位数字' }
  }
  if (!/^\d{6}$/.test(code)) {
    return { valid: false, message: '验证码必须为6位数字' }
  }
  return { valid: true }
}

