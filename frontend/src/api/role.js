import request from '@/utils/request'

export function getRoleList() {
  return request({
    url: '/roles/list',
    method: 'post'
  })
}

export function getRoleDetail(roleId) {
  return request({
    url: `/roles/${roleId}/detail`,
    method: 'post'
  })
}

