<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">사내 지식관리 시스템</h1>
      <DxForm :form-data="form" :col-count="1">
        <DxSimpleItem data-field="loginId" :label="{ text: '아이디' }"
          :editor-options="{ onValueChanged: e => form.loginId = e.value }">
          <DxRequiredRule />
        </DxSimpleItem>
        <DxSimpleItem data-field="password" editor-type="dxTextBox"
          :editor-options="{ mode: 'password', onValueChanged: e => form.password = e.value }"
          :label="{ text: '비밀번호' }">
          <DxRequiredRule />
        </DxSimpleItem>
      </DxForm>
      <DxButton
        text="로그인"
        type="default"
        styling-mode="contained"
        :width="'100%'"
        style="margin-top: 16px"
        @click="handleLogin"
      />
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DxForm, DxSimpleItem, DxRequiredRule } from 'devextreme-vue/form'
import { DxButton } from 'devextreme-vue/button'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ loginId: '', password: '' })
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  if (!form.loginId || !form.password) {
    errorMsg.value = '아이디와 비밀번호를 입력해주세요.'
    return
  }
  try {
    await auth.login(form.loginId, form.password)
    await auth.fetchMe()
    router.push('/spaces')
  } catch {
    errorMsg.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #F4F5F6;
  font-family: var(--font-family, "Pretendard GOV", "Pretendard", sans-serif);
}
.login-card {
  background: #FFFFFF;
  padding: 40px;
  border: 1px solid #B1B8BE;
  border-radius: 8px;
  width: 400px;
}
.login-title {
  text-align: center;
  margin-bottom: 32px;
  font-size: 24px;
  font-weight: 700;
  color: #1E2124;
  line-height: 1.3;
}
.error-msg {
  color: #DE3412;
  text-align: center;
  margin-top: 12px;
  font-size: 15px;
}
</style>
