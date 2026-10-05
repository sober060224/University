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
