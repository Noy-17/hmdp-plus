<script setup>
import { ref } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'
import router from '@/router'
import { userLogin } from '@/api/user'
const formRef = ref()
const userStore = useUserStore()
const form = ref({
  // phone: '13686869696',
  phone: '13838411438',
  password: '123456',
  radio: true
})

const goBack = () => {
  window.history.back()
}

const login = async () => {
  console.log('登录功能待实现')
  try {
    await formRef.value.validate()
    const res = await userLogin(form.value)
    console.log('登录成功:', res)
    console.log('登录成功token:', res.data)
    if (res.data) {
      userStore.setToken(res.data)
      console.log('token已设置:', userStore.getToken())
      ElMessage.success('登录成功')
      router.push('/index')
    } else {
      ElMessage.error('登录失败：未获取到token')
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录失败：' + (error.message || '未知错误'))
  }
}

// 添加表单校验规则
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  radio: [
    {
      required: true,
      message: '必选',
      trigger: 'change'
    }
  ]
}
</script>

<template>
  <div class="login-container">
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">密码登录&nbsp;&nbsp;&nbsp;</div>
    </div>
    <div class="content">
      <!-- 替换为 el-form -->
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="0"
        class="login-form"
      >
        <el-form-item prop="phone">
          <el-input placeholder="请输入手机号" v-model="form.phone"> </el-input>
        </el-form-item>
        <div style="height: 5px"></div>
        <el-form-item prop="password">
          <el-input
            placeholder="请输入密码"
            v-model="form.password"
            show-password
          >
          </el-input>
        </el-form-item>
        <div style="text-align: center; color: #8c939d; margin: 5px 0">
          <a href="javascript:void(0)">忘记密码</a>
        </div>
        <el-button
          @click="login"
          style="width: 100%; background-color: #f63; color: #fff"
          >登录</el-button
        >
        <div style="text-align: right; color: #333333; margin: 5px 0">
          <router-link to="/register">验证码登录</router-link>
        </div>
        <div class="login-radio">
          <el-form-item prop="radio">
            <el-checkbox v-model="form.radio" :true-value="1" :false-value="''">
            </el-checkbox>
          </el-form-item>
          <div>
            我已阅读并同意
            <a href="javascript:void(0)"> 《黑马点评用户服务协议》</a>、
            <a href="javascript:void(0)">《隐私政策》</a>
            等，接受免除或者限制责任、诉讼管辖约定等粗体标示条款
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
@import '@/assets/css/login.css';
</style>
