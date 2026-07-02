<template>
  <div class="error-page">
    <div class="error-content">
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.error-content {
  text-align: center;

  h1 {
    font-size: 80px;
    color: #909399;
    line-height: 1;
    margin-bottom: 16px;
  }

  p {
    font-size: 16px;
    color: #606266;
    margin-bottom: 24px;
  }
}
</style>
