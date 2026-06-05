<script setup>
import { ref } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'
import router from '@/router'
const disabled = ref(false) // 发送短信按钮
const codeBtnMsg = ref('发送验证码') // 发送短信按钮提示
const formRef = ref()

import { userGetCode, userLogin } from '@/api/user'

const form = ref({
  phone: '13686869696',
  // phone: '13838411438',
  code: '',
  radio: ''
})

// 修改为异步请求
const sendCode = async () => {
  try {
    await formRef.value.validateField('phone')
    const res = await userGetCode(form.value.phone)
    console.log('发送验证码:', res.data)
    form.value.code = res.data

    disabled.value = true
    codeBtnMsg.value = '60s后重发'
    let time = 60
    const timer = setInterval(() => {
      time--
      codeBtnMsg.value = time + 's后重发'
      if (time === 0) {
        clearInterval(timer)
        codeBtnMsg.value = '发送验证码'
        disabled.value = false
      }
    }, 1000)
  } catch (error) {
    console.error('验证码发送失败:', error)
    disabled.value = false
    codeBtnMsg.value = '发送验证码'
  }
}

const userStore = useUserStore()
const login = async () => {
  // 登录
  try {
    await formRef.value.validate()
    const res = await userLogin(form.value)
    console.log('注册成功token:', res.data)
    userStore.setToken(res.data)
    ElMessage.success('登录成功')
    router.push('/index')
  } catch (error) {
    console.error('登录失败:', error)
  }
}
const goBack = () => {
  console.log('goBack')
  window.history.back()
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur'
    }
  ],
  code: [{ required: true, message: '请输入验证码1', trigger: 'blur' }],
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
      <div class="header-title">手机号码快捷登录&nbsp;&nbsp;&nbsp;</div>
    </div>
    <div class="content">
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="0"
        class="login-form"
      >
        <el-form-item prop="phone">
          <div style="display: flex; justify-content: space-between">
            <el-input
              style="width: 60%"
              placeholder="请输入手机号"
              v-model="form.phone"
            >
            </el-input>
            <el-button
              style="width: 38%"
              @click="sendCode"
              type="success"
              :disabled="disabled"
              >{{ codeBtnMsg }}</el-button
            >
          </div>
        </el-form-item>

        <el-form-item prop="code">
          <el-input placeholder="请输入验证码" v-model="form.code"> </el-input>
        </el-form-item>

        <div style="text-align: center; color: #8c939d; margin: 5px 0">
          未注册的手机号码验证后自动创建账户
        </div>

        <el-form-item>
          <el-button
            @click="login"
            style="width: 100%; background-color: #f63; color: #fff"
            >登录</el-button
          >
        </el-form-item>

        <div style="text-align: right; color: #333333; margin: 5px 0">
          <router-link to="/login"> 密码登录 </router-link>
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
