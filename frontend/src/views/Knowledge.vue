<template>
  <div class="knowledge-container">
    <!-- 左侧：角色导航栏 -->
    <div class="role-sidebar" v-show="!isMobile || showRoleList">
      <div class="sidebar-header">
        <h2 class="sidebar-title">记忆对象</h2>
        <a-input-search 
          v-model="roleSearchKeyword" 
          placeholder="搜索角色..." 
          class="role-search"
          allow-clear
        />
      </div>

      <div class="role-list">
        <div 
          v-for="role in filteredRoles" 
          :key="role.id" 
          class="role-item"
          :class="{ active: currentRoleId === role.id }"
          @click="handleRoleChange(role.id)"
        >
          <a-avatar :size="40" :image-url="role.avatarUrl" class="role-avatar" />
          <div class="role-info">
            <div class="role-name">{{ role.name }}</div>
            <!-- 移除 roleSetting 展示 -->
          </div>
          <div class="active-indicator" :style="{ backgroundColor: role.themeColor || '#FF9A2E' }"></div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="filteredRoles.length === 0" class="empty-roles">
          无此角色
        </div>
      </div>
    </div>

    <!-- 右侧：内容管理区 -->
    <div class="content-area" v-show="!isMobile || !showRoleList">
      <template v-if="currentRole">
        <!-- 角色头部卡片 -->
        <div class="content-header" :style="{ backgroundImage: currentRole.bgImage }">
           <div class="header-overlay"></div>
           <div class="header-content">
             <a-button v-if="isMobile" type="text" shape="circle" class="mobile-back-btn" @click="handleBackToRoles">
               <icon-left />
             </a-button>
             <a-avatar :size="64" :image-url="currentRole.avatarUrl" class="header-avatar" :style="{ borderColor: currentRole.themeColor || '#fff' }" />
             <div class="header-text">
               <h1 class="current-role-name">{{ currentRole.name }} 的记忆库</h1>
               <p class="current-role-desc">投喂文档，让TA变得更懂你。</p>
             </div>
           </div>
        </div>

        <!-- 工具栏与表格 -->
        <div class="table-wrapper">
          <div class="toolbar">
            <div class="left-tools">
<a-upload
                :show-file-list="false"
                accept=".txt,.pdf,.md,.doc,.docx"
                :auto-upload="false"
                @change="handleFileSelect"
              >
                <template #upload-button>
                  <a-button type="primary" size="large" class="upload-btn" :style="{ backgroundColor: currentRole.themeColor, borderColor: currentRole.themeColor }">
                    <template #icon><icon-upload /></template>
                    上传记忆文档
                  </a-button>
                </template>
              </a-upload>
              <input ref="retryInputRef" type="file" accept=".txt,.pdf,.md,.doc,.docx" style="display:none" @change="handleRetryUpload" />
              <span class="tip-text">支持 PDF, Word, TXT (Max 10MB)</span>
            </div>
            
            <a-input-search 
              placeholder="搜索文档..." 
              style="width: 240px" 
              allow-clear
            />
          </div>

          <a-table
            :data="data"
            :loading="loading"
            :pagination="pagination"
            :bordered="false"
            row-key="fileId"
            class="custom-table"
            @page-change="(page)=>{ pagination.current = page; fetchDocs(); }"
            @page-size-change="(size)=>{ pagination.pageSize = size; pagination.current = 1; fetchDocs(); }"
          >
            <template #columns>
              <a-table-column title="文档名称" data-index="fileName">
                <template #cell="{ record }">
                  <div class="file-name-cell">
                    <icon-file-pdf v-if="record.fileName.endsWith('.pdf')" style="color: #ff4d4f; font-size: 24px; margin-right: 12px;" />
                    <icon-file v-else style="color: #1890ff; font-size: 24px; margin-right: 12px;" />
                    <span class="file-name-text">{{ record.fileName }}</span>
                  </div>
                </template>
              </a-table-column>
              <a-table-column title="解析进度" width="120">
                <template #cell="{ record }">
                  <div class="status-cell">
                    <a-tag v-if="getStatus(record) === 0" color="blue">解析中</a-tag>
                    <a-tag v-else-if="getStatus(record) === 1" color="green">解析完成</a-tag>
                    <a-tag v-else-if="getStatus(record) === 2" color="red">解析失败</a-tag>
                    <span v-else>--</span>
                  </div>
                </template>
              </a-table-column>
              <a-table-column title="大小" width="120">
                <template #cell="{ record }">
                  {{ record.fileSizeFormatted || (record.fileSize ? (record.fileSize / 1024 / 1024).toFixed(2) + ' MB' : '--') }}
                </template>
              </a-table-column>
              <a-table-column title="上传时间" width="180">
                <template #cell="{ record }">
                  {{ record.createTime ? dayjs(record.createTime).format('YYYY-MM-DD HH:mm') : '--' }}
                </template>
              </a-table-column>
              <a-table-column title="操作" width="200">
                <template #cell="{ record }">
                  <a-space>
                    <template v-if="getStatus(record) === 1">
                      <a-button type="text" size="small" shape="circle" @click="handlePreview(record)">
                        <icon-eye v-if="isImageFile(record.fileName) || isPdfFile(record.fileName)" />
                        <icon-download v-else />
                      </a-button>
                    </template>
                    <template v-else-if="getStatus(record) === 2">
                      <a-button type="text" size="small" shape="round" @click="handleRetry">
                        重新上传
                      </a-button>
                    </template>
                    <template v-else>
                      <a-tag color="blue">解析中</a-tag>
                    </template>
                    <a-popconfirm content="确定要删除这段记忆吗？" type="warning" @ok="handleDelete(record)">
                      <a-button type="text" size="small" status="danger" shape="circle">
                        <icon-delete />
                      </a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </a-table-column>
            </template>
            <template #empty>
               <div class="empty-state">
                 <div class="empty-icon-bg">
                   <icon-folder style="font-size: 32px; color: var(--color-text-3);" />
                 </div>
                 <p>空空如也~ 快去上传文档吧</p>
               </div>
            </template>
          </a-table>
        </div>
      </template>
      
      <!-- 未选择状态 (理论上默认会选中第一个，这里做防守) -->
      <div v-else class="no-select-state">
        <icon-user-group style="font-size: 48px; color: #ccc; margin-bottom: 16px;" />
        <p>请选择一个角色查看记忆库</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// 引入 vue h 函数用于 Modal
import { ref, onMounted, computed, h, reactive } from 'vue'
import dayjs from 'dayjs'
import { getDocList, deleteDoc } from '@/api/doc'
import { getRoleList } from '@/api/role'
import { Message, Modal } from '@arco-design/web-vue'
import { IconUpload, IconFile, IconFilePdf, IconDelete, IconFolder, IconUserGroup, IconEye, IconDownload, IconLeft } from '@arco-design/web-vue/es/icon'
import { adaptRoleData } from '@/utils/roleAdapter' // 引入适配器
import { fetchEventSource } from '@microsoft/fetch-event-source'

const isMobile = computed(() => window.innerWidth <= 768)
const showRoleList = ref(true)

const loading = ref(false)
const data = ref([])
const roles = ref([])
const currentRoleId = ref(null)
const roleSearchKeyword = ref('')
const pagination = reactive({ current: 1, pageSize: 8, total: 0 })
const uploadingDocId = ref(null)
const retryInputRef = ref(null)
const getStatus = (record) => {
  if (record.status === 0 || record.status === 1 || record.status === 2) return record.status
  if (record.parseStatus === 'parsing') return 0
  if (record.parseStatus === 'success') return 1
  if (record.parseStatus === 'fail') return 2
  return undefined
}
const reuploadTargetId = ref(null)
const hiddenFileInput = ref(null)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const currentRole = computed(() => {
  return roles.value.find(r => r.id === currentRoleId.value)
})

// 前端过滤角色列表
const filteredRoles = computed(() => {
  if (!roleSearchKeyword.value) return roles.value
  const kw = roleSearchKeyword.value.toLowerCase()
  return roles.value.filter(r => 
    r.name.toLowerCase().includes(kw)
    // 移除对 roleSetting 的搜索匹配，或者保留搜索但不展示
  )
})

const mapStatus = (status) => {
  if (status === 0) return 'parsing'
  if (status === 1) return 'success'
  if (status === 2) return 'fail'
  return ''
}

// 初始化
const init = async () => {
  try {
    const roleRes = await getRoleList()
    // 使用适配器处理数据，确保 visual 等字段可用
    roles.value = roleRes.map(adaptRoleData)
    
    if (roles.value.length > 0) {
      if (!isMobile.value) {
        currentRoleId.value = roles.value[0].id
        fetchDocs()
      }
    }
  } catch (error) {
    console.error(error)
  }
}

const fetchDocs = async () => {
  if (!currentRoleId.value) return
  
  loading.value = true
  try {
    const res = await getDocList({ roleId: currentRoleId.value, page: pagination.current, pageSize: pagination.pageSize })
    const list = res?.rows || res || []
    pagination.total = res?.total ?? list.length
    data.value = list.map(item => ({
      ...item,
      parseStatus: mapStatus(item.status)
    }))
  } catch (error) {
    // error
  } finally {
    loading.value = false
  }
}

// 辅助函数：判断文件类型
const isImageFile = (filename) => /\.(jpg|jpeg|png|gif|webp)$/i.test(filename)
const isPdfFile = (filename) => /\.pdf$/i.test(filename)

const handlePreview = async (record) => {
  if (!record.fileUrl) return
  
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/file/get`, {
        method: 'POST',
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json' 
        },
        body: JSON.stringify({ 
            objectKey: record.fileUrl, // 使用 fileUrl 作为 objectKey
            fileName: record.fileName
        })
    })
    
    const resData = await response.json()
    if (resData.code !== 200 || !resData.data?.url) {
        throw new Error(resData.message || '获取文件地址失败')
    }
    
    const fileUrl = resData.data.url

    // 图片预览
    if (isImageFile(record.fileName)) {
      Modal.info({
        title: record.fileName,
        content: () => h('img', { 
            src: fileUrl, 
            style: { maxWidth: '100%', maxHeight: '70vh', display: 'block', margin: '0 auto' } 
        }),
        width: 'auto',
        footer: false,
        closable: true,
        simple: false
      })
    } 
    // PDF 预览 (新窗口打开)
    else if (isPdfFile(record.fileName)) {
      window.open(fileUrl, '_blank')
    }
    // 其他文件直接下载
    else {
      handleDownload(record, fileUrl)
    }
  } catch (e) {
    Message.error(e.message || '操作失败')
  }
}

const handleDownload = (record, url) => {
    const a = document.createElement('a')
    a.href = url
    a.download = record.fileName || 'file'
    a.target = '_blank'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
}

const handleRoleChange = (id) => {
  currentRoleId.value = id
  pagination.current = 1
  showRoleList.value = false
  fetchDocs()
}

const handleBackToRoles = () => {
  showRoleList.value = true
  currentRoleId.value = null
}

const updateDocStatus = (fileId, status) => {
  const idx = data.value.findIndex(d => d.fileId === fileId)
  if (idx >= 0) {
    data.value[idx].status = status
  }
}

const uploadFile = async (file) => {
  if (!file || !currentRoleId.value) return

  const tempId = `temp_${Date.now()}`
  uploadingDocId.value = tempId
  data.value.unshift({
    fileId: tempId,
    fileName: file.name,
    fileSize: file.size,
    createTime: dayjs().format('YYYY-MM-DD HH:mm'),
    status: 0
  })

  const formData = new FormData()
  formData.append('file', file)
  formData.append('roleId', currentRoleId.value)

  try {
    await fetchEventSource('/api/doc/parse', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token') || ''}`
      },
      body: formData,
      onmessage(ev) {
        if (ev.event === 'progress') {
          if (ev.data === 'parsing') {
            updateDocStatus(uploadingDocId.value, 0)
          } else if (ev.data === 'success') {
            updateDocStatus(uploadingDocId.value, 1)
            Message.success('解析完成')
            fetchDocs()
          } else if (ev.data === 'fail') {
            updateDocStatus(uploadingDocId.value, 2)
            Message.error('解析失败')
          }
        }
        if (ev.event === 'done') {
          // 后端控制关闭，前端只清理本地状态
          uploadingDocId.value = null
          return
        }
      },
      onerror(err) {
        updateDocStatus(uploadingDocId.value, 2)
        Message.error('解析失败')
        throw err
      }
    })
  } catch (e) {
    updateDocStatus(uploadingDocId.value, 2)
  } finally {
    uploadingDocId.value = null
  }
}

const handleFileSelect = async (_, fileItem) => {
  const file = fileItem?.file
  if (!file) return
  uploadFile(file)
}

const handleRetry = () => {
  if (retryInputRef.value) {
    retryInputRef.value.value = ''
    retryInputRef.value.click()
  }
}

const handleRetryUpload = (e) => {
  const file = e.target.files && e.target.files[0]
  if (file) {
    uploadFile(file)
  }
  e.target.value = ''
}

const handleDelete = async (record) => {
  try {
    await deleteDoc({ fileId: record.fileId })
    Message.success('记忆已删除')
    fetchDocs()
  } catch (error) {
    // error
  }
}

onMounted(() => {
  init()
})
</script>

<style scoped>
.knowledge-container {
  display: flex;
  height: 100%;
  background-color: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 左侧导航栏 */
.role-sidebar {
  width: 280px;
  background-color: #FFFBF5; /* 极浅的米色背景 */
  border-right: 1px solid rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px 16px;
}

.sidebar-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #4E342E;
  padding-left: 4px;
}

.role-search {
  background-color: #fff;
  border-radius: 8px;
}

.role-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 20px 12px;
}

.role-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 12px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.role-item:hover {
  background-color: rgba(255, 154, 46, 0.08);
}

.role-item.active {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.role-avatar {
  border: 2px solid transparent;
  margin-right: 12px;
}

.role-item.active .role-avatar {
  border-color: var(--primary-6);
}

.role-info {
  flex: 1;
  overflow: hidden;
  /* 垂直居中 */
  display: flex; 
  align-items: center;
}

.role-name {
  font-size: 15px;
  font-weight: 600;
  color: #4E342E;
  /* margin-bottom: 2px; */ /* 只有一行文字了，不需要 margin */
}

.active-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 24px;
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.role-item.active .active-indicator {
  opacity: 1;
}

.empty-roles {
  text-align: center;
  color: #ccc;
  padding-top: 40px;
  font-size: 13px;
}

/* 右侧内容区 */
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.content-header {
  height: 160px;
  position: relative;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  padding: 0 40px;
}

.header-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
}

.header-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-avatar {
  border: 3px solid #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.current-role-name {
  margin: 0 0 6px 0;
  font-size: 24px;
  color: #4E342E;
}

.current-role-desc {
  margin: 0;
  color: #6D4C41;
  font-size: 14px;
}

.table-wrapper {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.left-tools {
  display: flex;
  align-items: center;
  gap: 16px;
}

.upload-btn {
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
}

.tip-text {
  color: #999;
  font-size: 12px;
}

.file-name-text {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.status-cell {
  display: flex;
  align-items: center;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #999;
}

.empty-icon-bg {
  width: 64px;
  height: 64px;
  background-color: #F7F8FA;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 16px;
}

/* 表格样式覆盖 */
:deep(.arco-table-cell) {
  padding: 14px 16px !important;
}
:deep(.arco-table-th) {
  background-color: #fff !important;
  border-bottom: 1px solid #f0f0f0;
  color: #8D6E63;
}
:deep(.arco-table-tr:hover .arco-table-td) {
  background-color: #FFFBF5 !important;
}

.mobile-back-btn {
  margin-right: 10px;
  color: #4E342E;
  background: rgba(255,255,255,0.6);
}

/* Mobile Adaptation */
@media (max-width: 768px) {
  .knowledge-container {
    flex-direction: column;
    border-radius: 0;
  }
  
  .role-sidebar {
    width: 100%;
    flex: 1;
    border-right: none;
  }
  
  .content-area {
    width: 100%;
    flex: 1;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 100;
  }
  
  .content-header {
    height: 120px;
    padding: 0 16px;
  }
  
  .current-role-name {
    font-size: 20px;
  }
  
  .current-role-desc {
    font-size: 12px;
  }
  
  .header-avatar {
    width: 48px !important;
    height: 48px !important;
  }
  
  .table-wrapper {
    padding: 16px;
  }
  
  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .left-tools {
    justify-content: space-between;
  }
  
  /* Mobile Table Card View */
  :deep(.arco-table-thead) {
    display: none;
  }
  
  :deep(.arco-table-tr) {
    display: flex;
    flex-direction: column;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 12px;
    margin-bottom: 12px;
    padding: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  }
  
  :deep(.arco-table-td) {
    border-bottom: none !important;
    padding: 4px 0 !important;
    background: transparent !important;
  }
  
  /* Custom layout for specific columns */
  /* Name column */
  :deep(.arco-table-td:nth-child(1)) {
    order: 1;
    font-weight: bold;
    margin-bottom: 8px;
  }
  
  /* Status column */
  :deep(.arco-table-td:nth-child(2)) {
    order: 2;
    display: flex;
    justify-content: space-between;
  }
  :deep(.arco-table-td:nth-child(2))::before {
    content: "状态";
    color: #999;
    font-size: 12px;
  }
  
  /* Size column */
  :deep(.arco-table-td:nth-child(3)) {
    order: 3;
    display: flex;
    justify-content: space-between;
  }
  :deep(.arco-table-td:nth-child(3))::before {
    content: "大小";
    color: #999;
    font-size: 12px;
  }
  
  /* Time column */
  :deep(.arco-table-td:nth-child(4)) {
    order: 4;
    display: flex;
    justify-content: space-between;
  }
  :deep(.arco-table-td:nth-child(4))::before {
    content: "时间";
    color: #999;
    font-size: 12px;
  }
  
  /* Action column */
  :deep(.arco-table-td:last-child) {
    order: 5;
    margin-top: 12px;
    padding-top: 12px !important;
    border-top: 1px solid #f0f0f0 !important;
    justify-content: flex-end;
    display: flex;
  }
}
</style>
