import Mock from 'mockjs'

// 模拟不同角色的文档数据
const docsMap = {
  1: [ // 小柴的文档
    {
      fileId: 'file-shiba-1',
      fileName: '日本京都旅行攻略.pdf',
      fileSize: 1024 * 1024 * 3.5,
      createTime: '2023-10-01 12:00:00'
    },
    {
      fileId: 'file-shiba-2',
      fileName: '柴犬最爱的零食清单.docx',
      fileSize: 1024 * 500,
      createTime: '2023-10-02 14:30:00'
    }
  ],
  2: [ // 布布的文档
    {
      fileId: 'file-ragdoll-1',
      fileName: '猫咪心理学入门.pdf',
      fileSize: 1024 * 1024 * 1.2,
      createTime: '2023-10-05 09:00:00'
    }
  ],
  3: [ // 阿尔法的文档
    {
      fileId: 'file-wolf-1',
      fileName: '狼群战术分析.pdf',
      fileSize: 1024 * 1024 * 5.0,
      createTime: '2023-10-06 16:20:00'
    },
    {
      fileId: 'file-wolf-2',
      fileName: '野外生存指南.txt',
      fileSize: 1024 * 10,
      createTime: '2023-10-06 16:25:00'
    }
  ]
}

export default [
  // 文档列表 (支持按 roleId 过滤)
  Mock.mock(/\/api\/doc\/list/, 'post', (options) => {
    const body = JSON.parse(options.body)
    const roleId = body.roleId || 1 // 默认角色1
    
    return {
      code: 200,
      message: 'success',
      data: docsMap[roleId] || []
    }
  }),

  // 上传解析
  Mock.mock(/\/api\/doc\/parse/, 'post', (options) => {
    // Mock环境下无法真正解析 FormData，这里简单模拟
    // 实际后端会接收 roleId 和 file
    
    return {
      code: 200,
      message: '上传成功',
      data: {
        fileId: 'file-' + Mock.Random.id(),
        fileName: '新上传的文档.pdf',
        fileSize: 1024 * 1024,
        createTime: Mock.Random.now()
      }
    }
  }),

  // 删除文档
  Mock.mock(/\/api\/doc\/delete/, 'post', () => {
    return {
      code: 200,
      message: '删除成功',
      data: '文档删除成功'
    }
  })
]
