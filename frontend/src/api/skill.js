import request from '@/utils/request'

// 获取技能执行历史
export function fetchSkillHistory(roleId, page = 1, pageSize = 10) {
  return request({
    url: '/skill/history',
    method: 'get',
    params: { roleId, page, pageSize }
  })
}

// 下载技能执行结果 Markdown
export function downloadSkillMd(historyId) {
  return request({
    url: `/skill/history/${historyId}/md`,
    method: 'get',
    responseType: 'blob'
  })
}

// 预览技能执行结果 Markdown（302 跳转到预签名链接）
export function previewSkillMd(historyId) {
  return request({
    url: `/skill/history/${historyId}/md/preview`,
    method: 'get'
  })
}

// 获取技能执行额度
export function getSkillLimit(roleId) {
  return request({
    url: '/skill/limit',
    method: 'get',
    params: { roleId }
  })
}

