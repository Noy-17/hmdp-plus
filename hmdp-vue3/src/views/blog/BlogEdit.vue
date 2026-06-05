<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores'
import {
  uploadBlogImage,
  deleteBlogImage,
  queryShopsByName,
  createBlog
} from '@/api/blog'
// import { showToast } from 'vant'

const router = useRouter()
const userStore = useUserStore()

// 数据定义
const fileInput = ref(null)
const fileList = ref([]) // 文件列表
const params = reactive({
  title: '',
  content: ''
})
const showDialog = ref(false)
const shops = ref([]) // 商户信息
const shopName = ref('') // 商户名称
const selectedShop = ref({}) // 选中的商户

// 生命周期钩子
onMounted(() => {
  checkLogin()
  queryShops()
})

// 方法定义
const checkLogin = () => {
  // 获取token
  const token = userStore.getToken()
  if (!token) {
    router.push('/login')
    return
  }

  // TODO: 查询用户信息
  // 这里可以添加获取用户信息的API调用
}

const queryShops = () => {
  queryShopsByName(shopName.value)
    .then((res) => {
      shops.value = res.data
    })
    .catch((err) => {
      ElMessage.error(err.message || '获取商户列表失败')
    })
}

const selectShop = (shop) => {
  selectedShop.value = shop
  showDialog.value = false
}

// const submitBlog = () => {
//   if (!params.title) {
//     ElMessage.warning('请输入标题')
//     return
//   }

//   if (!params.content) {
//     ElMessage.warning('请输入内容')
//     return
//   }

//   if (fileList.value.length === 0) {
//     ElMessage.warning('请上传至少一张图片')
//     return
//   }

//   if (!selectedShop.value.id) {
//     ElMessage.warning('请选择关联商户')
//     return
//   }

//   const data = {
//     ...params,
//     images: fileList.value.join(','),
//     shopId: selectedShop.value.id
//   }

//   createBlog(data)
//     .then(() => {
//       ElMessage.success('发布成功')
//       router.push('/infoHtml')
//     })
//     .catch((err) => {
//       ElMessage.error(err.message || '发布失败')
//     })
// }

const openFileDialog = () => {
  fileInput.value.click()
}

const fileSelected = (event) => {
  const file = event.target.files[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  uploadBlogImage(formData)
    .then((res) => {
      fileList.value.push('/imgs' + res.data)
    })
    .catch((err) => {
      ElMessage.error(err.message || '上传图片失败')
    })
}

const deletePic = (index) => {
  const imagePath = fileList.value[index]
  deleteBlogImage(imagePath)
    .then(() => {
      fileList.value.splice(index, 1)
    })
    .catch((err) => {
      ElMessage.error(err.message || '删除图片失败')
    })
}

const goBack = () => {
  router.back()
}

const publishBlog = async () => {
  if (!params.title) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!params.content) {
    ElMessage.warning('请输入内容')
    return
  }
  if (fileList.value.length === 0) {
    ElMessage.warning('请上传图片')
    return
  }

  try {
    const blogData = {
      title: params.title,
      content: params.content,
      images: fileList.value.join(','),
      shopId: selectedShop.value ? selectedShop.value.id : null
    }

    await createBlog(blogData)
    ElMessage.success('发布成功')
    router.push('/infoHtml')
  } catch (error) {
    console.error('发布失败', error)
    ElMessage.error('发布失败')
  }
}
</script>

<template>
  <div class="blog-edit">
    <div class="header">
      <div class="left" @click="goBack">
        <i class="iconfont icon-arrow-left"></i>
      </div>
      <div class="title">发布笔记</div>
      <div class="right" @click="publishBlog">发布</div>
    </div>

    <div class="upload-box">
      <input
        type="file"
        @change="fileSelected"
        name="file"
        ref="fileInput"
        style="display: none"
      />
      <div class="upload-btn" @click="openFileDialog">
        <i class="iconfont icon-camera"></i>
        <span>上传图片</span>
      </div>
      <div class="pic-list" v-if="fileList.length > 0">
        <div class="pic-box" v-for="(f, i) in fileList" :key="i">
          <img :src="`/src/assets${f}`" alt="图片" />
          <div class="delete" @click="deletePic(i)">
            <i class="iconfont icon-close"></i>
          </div>
        </div>
      </div>
    </div>

    <div class="blog-title">
      <input type="text" v-model="params.title" placeholder="标题" />
    </div>

    <div class="blog-content">
      <textarea
        v-model="params.content"
        placeholder="分享你的美食体验..."
      ></textarea>
    </div>

    <div class="divider"></div>

    <div class="blog-shop" @click="showDialog = true">
      <div class="shop-left">关联商户</div>
      <div v-if="selectedShop.name">{{ selectedShop.name }}</div>
      <div v-else>
        去选择&nbsp;<el-icon><ArrowRight /></el-icon>
      </div>
    </div>

    <div class="mask" v-show="showDialog" @click="showDialog = false"></div>

    <el-dialog
      v-model="showDialog"
      title="选择商户"
      width="90%"
      :show-close="false"
      :close-on-click-modal="false"
    >
      <div class="search-bar">
        <div class="city-select">
          杭州 <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="search-input">
          <el-icon @click="queryShops"><Search /></el-icon>
          <el-input
            v-model="shopName"
            placeholder="搜索商户名称"
            @keyup.enter="queryShops"
          ></el-input>
        </div>
      </div>

      <div class="shop-list">
        <div
          v-for="s in shops"
          :key="s.id"
          class="shop-item"
          @click="selectShop(s)"
        >
          <div class="shop-name">{{ s.name }}</div>
          <div>{{ s.area }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
@import '@/assets/css/blog-edit.css';
</style>
