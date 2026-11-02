import { describe, it, expect } from 'vitest'
import {
    toApiPage,
    protectionColor,
    PROTECTION_LEVELS,
    PROTECTION_FALLBACK_COLOR,
    safeExternalUrl,
    safeRedirect,
    fetchAllPages,
    formatTime,
    toInputTime,
    nowInputTime
} from './index'

// 这些是 R3/R5 修过的安全函数与时间换算，回归价值最高：
// 一旦有人「简化」掉解码后再判的步骤，或把 toInputTime 改回 slice(0,16)，
// 下面的用例会立刻红。

describe('safeRedirect（R3 High：开放重定向）', () => {
    it('站内绝对路径原样放行', () => {
        expect(safeRedirect('/dashboard')).toBe('/dashboard')
        expect(safeRedirect('/species/12')).toBe('/species/12')
    })

    it('协议相对 //host 被挡（这是最常被利用的一种）', () => {
        expect(safeRedirect('//evil.com')).toBe('/dashboard')
        expect(safeRedirect('//evil.com/path')).toBe('/dashboard')
    })

    it('反斜杠变体被挡（浏览器把 /\\ 当 //）', () => {
        expect(safeRedirect('/\\evil.com')).toBe('/dashboard')
    })

    it('编码后的 //host 被挡（先解码再判）', () => {
        // 字面上首字符是 /，第二个字符是 %，只判原文会放过去；
        // 浏览器/router 解码后变成 //evil.com
        expect(safeRedirect('/%2Fevil.com')).toBe('/dashboard')
        expect(safeRedirect('/%5Cevil.com')).toBe('/dashboard')
    })

    it('绝对 URL / javascript: 被挡', () => {
        expect(safeRedirect('https://evil.com')).toBe('/dashboard')
        expect(safeRedirect('javascript:alert(1)')).toBe('/dashboard')
        expect(safeRedirect('evil.com')).toBe('/dashboard')
    })

    it('含空白与控制字符的路径被挡', () => {
        expect(safeRedirect('/a b')).toBe('/dashboard')
        expect(safeRedirect('/a\tb')).toBe('/dashboard')
        expect(safeRedirect('/a\u0000b')).toBe('/dashboard')
    })

    it('非字符串与空值退回 fallback', () => {
        expect(safeRedirect(undefined)).toBe('/dashboard')
        expect(safeRedirect(null)).toBe('/dashboard')
        expect(safeRedirect(123)).toBe('/dashboard')
        expect(safeRedirect('')).toBe('/dashboard')
        expect(safeRedirect('/x', '/login')).toBe('/x')
    })

    it('解码失败（畸形百分号）时退回 fallback 而不是抛异常', () => {
        expect(safeRedirect('/%E0%A4%A')).toBe('/dashboard')
    })
})

describe('safeExternalUrl（R3/R5：javascript: 协议注入）', () => {
    it('http / https 放行', () => {
        expect(safeExternalUrl('http://a.cn/x')).toBe('http://a.cn/x')
        expect(safeExternalUrl('https://a.cn/x')).toBe('https://a.cn/x')
        expect(safeExternalUrl('  https://a.cn/x  ')).toBe('https://a.cn/x')
    })

    it('javascript: / data: / file: 一律返回空串', () => {
        expect(safeExternalUrl('javascript:alert(1)')).toBe('')
        expect(safeExternalUrl('JavaScript:alert(1)')).toBe('')
        expect(safeExternalUrl('data:text/html,<script>')).toBe('')
        expect(safeExternalUrl('file:///etc/passwd')).toBe('')
    })

    it('不含协议的相对地址返回空串（详情页只接受绝对外链）', () => {
        expect(safeExternalUrl('www.a.cn')).toBe('')
        expect(safeExternalUrl('/local.jpg')).toBe('')
    })

    it('含空白的串被挡（防 "https://a.cn javascript:..." 这类拼接）', () => {
        expect(safeExternalUrl('https://a.cn x')).toBe('')
    })

    it('null / undefined 返回空串，不抛异常', () => {
        expect(safeExternalUrl(null)).toBe('')
        expect(safeExternalUrl(undefined)).toBe('')
    })
})

describe('toApiPage（页面 1 基 → 接口 0 基）', () => {
    it('1 → 0，2 → 1', () => {
        expect(toApiPage(1)).toBe(0)
        expect(toApiPage(2)).toBe(1)
    })

    it('0 与负数被夹到 0（不会发出 page=-1 的请求）', () => {
        expect(toApiPage(0)).toBe(0)
        expect(toApiPage(-5)).toBe(0)
    })
})

describe('protectionColor（R5：图例与种子数据对齐）', () => {
    it('已知等级返回各自配色', () => {
        expect(protectionColor('国家一级')).toBe('#e4572e')
        expect(protectionColor('无')).toBe('#0a6e96')
    })

    it('未知/空等级落到兜底色，不会 undefined', () => {
        expect(protectionColor('不存在的等级')).toBe(PROTECTION_FALLBACK_COLOR)
        expect(protectionColor(null)).toBe(PROTECTION_FALLBACK_COLOR)
        expect(protectionColor(undefined)).toBe(PROTECTION_FALLBACK_COLOR)
    })

    it('protectionLevels 里每一项都有颜色（表与地图上色同源）', () => {
        for (const item of PROTECTION_LEVELS) {
            expect(item.color).toMatch(/^#[0-9a-f]{6}$/i)
            expect(protectionColor(item.name)).toBe(item.color)
        }
    })
})

describe('formatTime', () => {
    it('格式化成本地可读形式', () => {
        // 不带时区的 LocalDateTime 串按本地时间解释
        expect(formatTime('2026-09-30T15:35:25')).toBe('2026-09-30 15:35')
    })

    it('空值返回空串（表格不会显示 undefined）', () => {
        expect(formatTime(null)).toBe('')
        expect(formatTime('')).toBe('')
        expect(formatTime(undefined)).toBe('')
    })

    it('无法解析的串原样返回，不抛异常', () => {
        expect(formatTime('不是时间')).toBe('不是时间')
    })
})

describe('toInputTime（R5：带时区的串不能截断成 UTC）', () => {
    it('不带时区的串直接截断到分钟', () => {
        expect(toInputTime('2026-09-30T15:35:25')).toBe('2026-09-30T15:35')
    })

    it('带 Z 的串按本地时区换算（不能 slice 出 UTC 时间）', () => {
        const utc = '2026-09-30T07:35:25Z'
        const local = new Date(utc)
        const expected = `${local.getFullYear()}-${String(local.getMonth() + 1).padStart(2, '0')}`
            + `-${String(local.getDate()).padStart(2, '0')}T`
            + `${String(local.getHours()).padStart(2, '0')}:${String(local.getMinutes()).padStart(2, '0')}`
        expect(toInputTime(utc)).toBe(expected)
    })

    it('带 +08:00 偏移的串同样按本地换算', () => {
        const value = '2026-09-30T15:35:25+08:00'
        const local = new Date(value)
        const expected = `${local.getFullYear()}-${String(local.getMonth() + 1).padStart(2, '0')}`
            + `-${String(local.getDate()).padStart(2, '0')}T`
            + `${String(local.getHours()).padStart(2, '0')}:${String(local.getMinutes()).padStart(2, '0')}`
        expect(toInputTime(value)).toBe(expected)
    })

    it('空值返回空串', () => {
        expect(toInputTime(null)).toBe('')
        expect(toInputTime('')).toBe('')
    })
})

describe('nowInputTime', () => {
    it('输出 datetime-local 需要的 yyyy-MM-ddTHH:mm 形状', () => {
        expect(nowInputTime()).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/)
    })

    it('与本地墙钟一致（不是 UTC）', () => {
        const now = new Date()
        const expectedHour = String(now.getHours()).padStart(2, '0')
        expect(nowInputTime().slice(11, 13)).toBe(expectedHour)
    })
})

describe('fetchAllPages（R5：导出不能只导第一页）', () => {
    it('按 total 拉全所有页', async () => {
        const all = Array.from({ length: 250 }, (_, i) => ({ id: i }))
        const calls = []
        const listFn = async ({ page, size }) => {
            calls.push({ page, size })
            return { records: all.slice(page * size, page * size + size), total: all.length }
        }

        const rows = await fetchAllPages(listFn, { keyword: 'x' })

        expect(rows).toHaveLength(250)
        expect(rows[0].id).toBe(0)
        expect(rows.at(-1).id).toBe(249)
        // 250 条 / 每页 100 → 3 页
        expect(calls).toHaveLength(3)
        expect(calls.map((c) => c.page)).toEqual([0, 1, 2])
        expect(calls.every((c) => c.size === 100)).toBe(true)
        // 筛选条件必须透传到每一页，否则第 2 页起变成全量
        expect(calls.every((c) => c.size === 100)).toBe(true)
    })

    it('记录数为 0 时只发一次请求', async () => {
        const calls = []
        const listFn = async ({ page }) => {
            calls.push(page)
            return { records: [], total: 0 }
        }
        const rows = await fetchAllPages(listFn, {})
        expect(rows).toHaveLength(0)
        expect(calls).toEqual([0])
    })

    it('恰好整除时不多发一页空请求', async () => {
        const all = Array.from({ length: 200 }, (_, i) => ({ id: i }))
        let count = 0
        const listFn = async ({ page, size }) => {
            count++
            return { records: all.slice(page * size, page * size + size), total: 200 }
        }
        const rows = await fetchAllPages(listFn, {})
        expect(rows).toHaveLength(200)
        expect(count).toBe(2)
    })

    it('records 为 null 时不抛异常（后端理论上不会给，但接口一改就会）', async () => {
        const listFn = async () => ({ records: null, total: 0 })
        await expect(fetchAllPages(listFn, {})).resolves.toHaveLength(0)
    })
})
