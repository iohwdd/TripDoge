import { useState, useCallback, useRef } from 'react'
import { message } from 'antd'
import { sendEmailCode } from '@/api/user/auth'
import { validateEmail } from '@/utils/validation'

/**
 * 邮箱验证码Hook
 * 提供发送验证码、倒计时等功能
 */
export const useEmailCode = () => {
  const [codeLoading, setCodeLoading] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  /**
   * 发送验证码
   * @param email 邮箱地址
   * @param onSuccess 发送成功回调（可选）
   */
  const sendCode = useCallback(
    async (email: string, onSuccess?: (code?: string) => void) => {
      // 验证邮箱格式
      if (!email) {
        message.warning('请先输入邮箱')
        return false
      }

      if (!validateEmail(email)) {
        message.error('请输入正确的邮箱格式')
        return false
      }

      // 如果正在倒计时，不允许重复发送
      if (countdown > 0) {
        message.warning('请稍后再试')
        return false
      }

      try {
        setCodeLoading(true)
        const res = await sendEmailCode({ email })
        if (res.code === 200) {
          message.success('验证码已发送，请查收邮箱')
          // 开发模式下显示验证码
          if (import.meta.env.DEV && res.data?.code) {
            message.info(`开发模式验证码: ${res.data.code}`)
          }

          // 开始倒计时
          setCountdown(60)
          // 清除之前的定时器
          if (timerRef.current) {
            clearInterval(timerRef.current)
          }
          // 创建新的定时器
          timerRef.current = setInterval(() => {
            setCountdown(prev => {
              if (prev <= 1) {
                if (timerRef.current) {
                  clearInterval(timerRef.current)
                  timerRef.current = null
                }
                return 0
              }
              return prev - 1
            })
          }, 1000)

          // 调用成功回调
          onSuccess?.(res.data?.code)
          return true
        }
        return false
      } catch (error) {
        console.error('发送验证码失败:', error)
        return false
      } finally {
        setCodeLoading(false)
      }
    },
    [countdown]
  )

  /**
   * 清除倒计时
   */
  const clearCountdown = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
    setCountdown(0)
  }, [])

  return {
    codeLoading,
    countdown,
    sendCode,
    clearCountdown,
  }
}
