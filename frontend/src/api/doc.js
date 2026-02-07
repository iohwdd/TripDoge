import request from '@/utils/request'

export function getDocList(data) {
  return request({
    url: '/doc/list',
    method: 'post',
    data
  })
}

export function uploadDoc(data) {
  return request({
    url: '/doc/parse',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function deleteDoc(data) {
  return request({
    url: '/doc/delete',
    method: 'post',
    data
  })
}

