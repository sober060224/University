import writeXlsxFile from 'write-excel-file/browser'

/**
 * 页面页码与接口页码的换算。
 * 页面上按 1 基显示和计数（Pagination 组件也是 1 基），而列表接口直接用
 * PageResult 的 PageRequest 页码，是 0 基，所有请求都要过这里换算一次。
 */
export function toApiPage(page) {
    return Math.max(page - 1, 0)
}

/**
 * 保护等级的统一取值表与配色。
 *
 * 这里是「物种管理表单的下拉选项」「地图按保护等级上色」「图例」三处共用的唯一来源。
 * 原来 MapView 里写的是 一级保护/二级保护/重点保护/一般保护，而 database.sql 的种子数据
 * 与 AiService 的提示词用的是 国家二级 / 自治区重点 / 无 这一套，两边一个都对不上：
 * 于是地图上每个点都落到兜底的灰色，图例列出四个永远没有数据的类别，
 * 而编辑一个已有物种时下拉框是空的（存的值不在选项里）。
 */
export const PROTECTION_LEVELS = [
    { name: '国家一级', color: '#e4572e' },
    { name: '国家二级', color: '#f0762b' },
    { name: '国家重点保护', color: '#f0a500' },
    { name: '自治区重点', color: '#c9a227' },
    { name: '有重要生态、科学、社会价值', color: '#0e9c9c' },
    { name: '无', color: '#0a6e96' }
]

/** 未登记的保护等级用的兜底色，保证它在图上仍然看得见 */
export const PROTECTION_FALLBACK_COLOR = '#5b6b78'

/**
 * 濒危度取值，必须与 database.sql 的 endanger_status 以及后端
 * SpeciesForms.SpeciesCreate 上的 @Pattern / AiService 的 TAXONOMY_PROMPT 完全一致。
 * 放在这里而不是各页面各抄一份：筛选条件对物种表是**等值**匹配
 * （SpeciesRepository.search 里是 `s.endangerStatus = :endangerStatus`，不是 like），
 * 手打的「危」「易危物种」这类近似词会直接查空，用户以为库里没有。
 */
export const ENDANGER_LEVELS = ['未评估', '无危', '近危', '易危', '濒危', '极危']

export function protectionColor(level) {
    const hit = PROTECTION_LEVELS.find((item) => item.name === level)
    return hit ? hit.color : PROTECTION_FALLBACK_COLOR
}

/**
 * 只放行 http(s) 绝对地址。
 *
 * 物种的「影像资料」是科研人员手填的自由文本，直接绑到 <a :href> 上时，
 * javascript: 开头的值会在点击时当作脚本执行（Vue 不会替你过滤这个协议），
 * 而详情页对未登录访客也是开放的——等于一个低权限用户填一条记录，
 * 就能在管理员点开链接的那一刻以管理员身份发请求。
 */
export function safeExternalUrl(value) {
    return /^https?:\/\/[^\s]+$/i.test(String(value ?? '').trim()) ? String(value).trim() : ''
}

/**
 * 校验登录页 ?redirect= 参数给出的站内路径，不安全就退回默认落地页。
 *
 * 这个参数原来是不加判断直接丢给 router.push 的。vue-router 会把 //evil.com
 * 当成一个合法路径，浏览器再把它按当前协议解析成 http://evil.com ——
 * 于是「登录成功后自动跳转」变成了一个现成的钓鱼跳转：发一条
 * /login?redirect=//attacker.tld 的链接，用户在已经登录的浏览器里点一下就被送到外部站点。
 * 因此只接受以单个 / 开头的站内绝对路径（顺带挡掉 //host 与反斜杠变体），其余一律不要用。
 */
export function safeRedirect(target, fallback = '/dashboard') {
    const value = typeof target === 'string' ? target.trim() : ''
    const isInternalPath = /^\/(?!\/|\\)/.test(value) && !/[\s\u0000-\u001f]/.test(value)
    return isInternalPath ? value : fallback
}

/**
 * 按 total 分页拉全量记录，交给导出工具生成 .xlsx（page 是 0 基）。
 *
 * 所有「导出当前列表」的入口都必须走这里，不能直接拿页面上的 rows：
 * 列表页每页只有 10 条，而导出按钮旁边就写着「共 N 条记录」，
 * 只导第一页等于静默丢掉 N-10 行，用户不会收到任何提示。
 * 后端会把 size 夹到上限 100，所以这里的页大小也取 100，与夹值一致。
 */
export async function fetchAllPages(listFn, params) {
    const first = await listFn({ ...params, page: 0, size: 100 })
    const all = [...(first.records || [])]
    const pages = Math.ceil((first.total || 0) / 100)
    for (let p = 1; p < pages; p++) {
        const next = await listFn({ ...params, page: p, size: 100 })
        all.push(...(next.records || []))
    }
    return all
}

/**
 * 导出为真正的 .xlsx 文件。
 * columns: [{ header: '中文名', key: 'chineseName', width: 20 }]
 * rows:    接口返回的原始对象数组
 */
export async function exportExcel(fileName, sheetName, columns, rows) {
    const data = [
        columns.map((c) => ({ value: c.header, fontWeight: 'bold' })),
        ...rows.map((row) => columns.map((c) => {
            const raw = row[c.key]
            return { value: raw === null || raw === undefined ? '' : String(raw), type: String }
        }))
    ]
    const result = await writeXlsxFile(data, {
        sheet: sheetName,
        columns: columns.map((c) => ({ width: c.width || 16 }))
    })
    await result.toFile(`${fileName}.xlsx`)
}

/**
 * 导出 PDF：调用浏览器打印，另存为 PDF 即可。
 * 打印时样式表里的 @media print 会自动隐藏导航、按钮和分页。
 */
export function exportPdf() {
    window.print()
}

const pad = (n) => String(n).padStart(2, '0')

/** 把 ISO-8601 时间串格式化成中文可读形式 */
export function formatTime(value) {
    if (!value) {
        return ''
    }
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
        return String(value)
    }
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
        + `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** <input type="datetime-local"> 需要 yyyy-MM-ddTHH:mm 形式 */
export function toInputTime(value) {
    return value ? String(value).slice(0, 16) : ''
}

/**
 * 当前本地时间，用作 datetime-local 的默认值。
 * 不能拿 toISOString() 切片：那是 UTC，东八区会比本地时间早 8 小时。
 */
export function nowInputTime() {
    const now = new Date()
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
        + `T${pad(now.getHours())}:${pad(now.getMinutes())}`
}
