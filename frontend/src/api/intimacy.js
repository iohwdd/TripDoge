import request from '@/utils/request'

export function getIntimacy(roleId) {
  return request({
    url: `/intimacy/${roleId}`,
    method: 'get'
  })
}

