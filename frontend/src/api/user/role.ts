// 角色相关API
import { userRequest } from './request'
import type { RoleInfoVO, RoleDetailVO, RoleListQueryDTO } from '@/types/role'

/**
 * 获取角色列表
 * @param queryDTO 查询参数（可选）
 */
export const getRoleList = (queryDTO?: RoleListQueryDTO) => {
  return userRequest.post<RoleInfoVO[]>('/api/roles/list', queryDTO)
}

/**
 * 获取角色详情
 * @param roleId 角色ID
 */
export const getRoleDetail = (roleId: number) => {
  return userRequest.post<RoleDetailVO>(`/api/roles/${roleId}/detail`)
}

