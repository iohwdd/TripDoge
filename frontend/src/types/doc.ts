// 文档相关类型定义

// 文档信息VO
export interface DocVO {
  id: number
  fileId: string
  userId: number
  roleId?: number
  fileUrl: string
  fileName: string
  fileSize: number
  fileSizeFormatted?: string
  createTime: string
  updateTime: string
}

// 文档上传结果VO
export interface DocUploadResultVO {
  doc: DocVO
  vectorizationSuccess: boolean
  vectorizationError?: string
  isConfigError?: boolean
}

// 文档列表查询DTO
export interface DocListDTO {
  roleId?: number
}

// 文档删除DTO
export interface DocDelDTO {
  docId: number
}

// 文档删除结果DTO
export interface DocDeleteResultDTO {
  success: boolean
  message?: string
}

// 文档下载DTO
export interface DocDownloadDTO {
  docId: number
}

// 文件上传DTO
export interface UploadDTO {
  file: File
  roleId?: number
}
