<script setup>
// 模块五 能力四：智能问答与科研助手
// 用户用自然语言提问，后端先把问题转成结构化查询去模块二/三检索数据，
// 再把检索结果作为上下文交给大模型生成回答 —— 避免模型凭空编造物种数据。
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { aiApi } from '../api/index'
import { formatTime } from '../utils/index'

const messages = ref([])
const question = ref('')
const asking = ref(false)
const error = ref('')
const bodyEl = ref(null)

const EXAMPLES = [
  '最近三年在湛江附近观测到的濒危物种有哪些？',
  '请总结红树林生态系统中物种数量的变化趋势',
  '系统中一共有多少个物种？保护等级如何分布？'
]

async function scrollToBottom() {
  await nextTick()
  if (bodyEl.value) {
    bodyEl.value.scrollTop = bodyEl.value.scrollHeight
  }
}

// 历史记录与实时发送的两条写路径会互相覆盖：loadHistory 回包晚到时
// messages.value = [...] 会把刚发出的提问与回答整体抹掉。序号丢弃过期回包。
let historyToken = 0
let sending = 0

async function loadHistory() {
  const token = ++historyToken
  try {
    const list = await aiApi.history()
    // 失败不能只留空数组：那会让空状态提示「先试试下面这些例子」，
    // 用户以为是自己第一次用，实际是历史记录没拉到。
    if (token !== historyToken) return
    // 用户在历史回包前已经发了新问题：把本地已有气泡接在后面，而不是覆盖
    messages.value = (list || []).map(m => ({
      role: m.role,
      content: m.content,
      time: formatTime(m.createTime)
    })).concat(sending ? messages.value.slice(-sending) : [])
    error.value = ''
  } catch (e) {
    if (token === historyToken) error.value = `历史记录加载失败：${e.message}`
  }
  scrollToBottom()
}

async function send(text) {
  const ask = (text ?? question.value).trim()
  if (!ask || asking.value) return
  asking.value = true
  error.value = ''
  question.value = ''
  // 先把用户气泡渲染出来，等待感比等接口返回后再出现要好
  messages.value.push({ role: 'user', content: ask, time: formatTime(new Date().toISOString()) })
  sending++
  scrollToBottom()
  try {
    const res = await aiApi.ask({ question: ask })
    messages.value.push({
      role: 'assistant',
      content: res?.answer || '（大模型没有返回内容）',
      time: formatTime(new Date().toISOString())
    })
  } catch (e) {
    // 只写 error 会让这条提问悬空：会话里既没有回答也没有失败标记，
    // 用户清掉错误提示后也无从重试。补一条可重试的失败气泡。
    messages.value.push({ role: 'assistant', content: `回答失败：${e.message}`, time: formatTime(new Date().toISOString()), failed: true, retry: ask })
  } finally {
    asking.value = false
    scrollToBottom()
  }
}

async function clearHistory() {
  if (!confirm('确定要清空当前账号的问答历史吗？')) return
  try {
    await aiApi.clearHistory()
    messages.value = []
    error.value = ''
  } catch (e) {
    // 清空失败时不能本地抹掉列表，否则界面与服务端不一致，刷新后又回来了
    error.value = e.message
  }
}

onMounted(loadHistory)

// 卸载后回包仍会 push 到 messages 并操作已销毁的 DOM
onBeforeUnmount(() => {
  historyToken++
  asking.value = false
})
</script>

<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">智能问答与科研助手</h2>
        <p class="page__desc">模块五能力四：自然语言提问，系统先从物种库与观测记录中检索真实数据，再由大模型组织回答</p>
      </div>
      <div class="page__actions">
        <button class="btn btn--ghost" :disabled="!messages.length" @click="clearHistory">清空历史</button>
      </div>
    </div>

    <div v-if="error" class="alert alert--error">{{ error }}</div>

    <div class="card" style="padding: 0">
      <div ref="bodyEl" class="chat__body">
        <div v-if="!messages.length" class="chat__empty">
          <p>还没有对话记录。可以先试试下面这些例子：</p>
          <button v-for="item in EXAMPLES" :key="item" type="button" class="chat__example"
                  :disabled="asking" @click="send(item)">{{ item }}</button>
        </div>
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="chat__msg"
          :class="{ 'chat__msg--me': m.role === 'user' }"
        >
          <div class="chat__avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
          <div class="chat__bubble">
            {{ m.content }}
            <div class="chat__foot">{{ m.time }}</div>
          </div>
        </div>
        <div v-if="asking" class="chat__msg">
          <div class="chat__avatar">AI</div>
          <div class="chat__bubble">正在检索系统数据并生成回答，请稍候…</div>
        </div>
      </div>

      <div class="chat__input">
        <textarea
          v-model="question"
          class="textarea"
          rows="2"
          placeholder="请输入你的问题，例如：最近三年在湛江附近观测到的濒危物种有哪些？"
          @keydown.enter.exact.prevent="send()"
        ></textarea>
        <button class="btn" :disabled="asking || !question.trim()" @click="send()">
          {{ asking ? '思考中…' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat__body {
  min-height: 420px;
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
}

.chat__empty {
  color: var(--text-sub);
  text-align: center;
  padding: 48px 16px;
}

.chat__example {
  display: block;
  width: 100%;
  margin: 8px auto;
  padding: 8px 12px;
  background: var(--primary-light);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--primary-dark);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
}

.chat__example:hover {
  border-color: var(--primary);
}

.chat__input {
  display: flex;
  gap: 10px;
  align-items: stretch;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
}

.chat__input .textarea {
  flex: 1;
  min-height: 56px;
  resize: vertical;
}
</style>
