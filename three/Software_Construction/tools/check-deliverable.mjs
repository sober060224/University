// 交付前自查：图与源文件齐备、表格结构、用例覆盖，以及课程报告模板的图表编号与引用条款
// 用法：node tools/check-deliverable.mjs 需求规范说明书-模块四-数据可视化与报表.md
import { readFileSync, existsSync, readdirSync } from 'node:fs';

const MD = process.argv[2];
const md = readFileSync(MD, 'utf8');
const lines = md.split(/\r?\n/);
let fail = 0;
const bad = (msg) => { console.log('   ✗ ' + msg); fail++; };

// ============ 1. 图与源文件是否齐备（以磁盘上的实际产物为期望集合）============
const svgs = readdirSync('diagrams').filter(f => f.endsWith('.svg')).sort();
const drawios = readdirSync('diagrams').filter(f => f.endsWith('.drawio') && f !== '模块四-全部图表.drawio').sort();
for (const f of svgs) if (!md.includes('diagrams/' + f)) bad(`报告未嵌入 ${f}`);
for (const f of drawios) if (!md.includes(f.replace('.drawio', ''))) bad(`报告未给出 ${f} 的源文件引用`);
const refImgs = [...md.matchAll(/!\[[^\]]*\]\(([^)]+)\)/g)].map(m => m[1]);
const missingImg = refImgs.filter(p => !existsSync(p));
console.log(`嵌入图 ${refImgs.length} 处 / 磁盘 svg ${svgs.length} 张：${missingImg.length ? '缺失 ' + missingImg.join(', ') : '引用均存在'}`);
fail += missingImg.length;
const refSrc = new Set([...md.matchAll(/diagrams\/[^\s`)|]+\.drawio/g)].map(m => m[0]));
const missingSrc = [...refSrc].filter(p => !existsSync(p));
console.log(`.drawio 源引用 ${refSrc.size} 个：${missingSrc.length ? '缺失 ' + missingSrc.join(', ') : '全部存在'}`);
fail += missingSrc.length;

// ============ 2. 表格列数一致 ============
let cols = -1;
const colBad = [];
lines.forEach((l, i) => {
  if (/^\s*\|.*\|\s*$/.test(l)) {
    const n = (l.match(/\|/g) || []).length;
    if (cols < 0) cols = n;
    else if (n !== cols) colBad.push(`${i + 1} 行：${n} vs 表头 ${cols} → ${l.slice(0, 40)}`);
  } else if (cols >= 0 && l.trim() !== '') cols = -1;
});
console.log(`表格列数：${colBad.length ? colBad.length + ' 行不一致' : '全部一致'}`);
colBad.slice(0, 8).forEach(b => bad(b));

// ============ 3. 章骨架 = 实验2 列出的四块 + 其他必要内容（期望集合写死）============
const CHAPTERS = [
  ['# 1 系统缩写表', '系统缩写表'],
  ['# 2 核心功能需求', '核心功能需求（用例图 + 用例规约 + 活动图）'],
  ['# 3 非功能性需求', '非功能性需求'],
  ['# 4 数据字典与规则表', '数据字典与规则表'],
  ['# 5 其他必要的需求分析内容', '其他必要的需求分析内容'],
];
for (const [re, name] of CHAPTERS) {
  if (!lines.some(l => l.startsWith(re))) bad(`缺少实验2 要求的文档组成章节：${name}（应有一级标题「${re}」`);
}
// 实验要求里没有这些栏目，出现即为自行加戏
for (const forbidden of ['参考文献', '变更记录']) {
  if (new RegExp('^#{1,3} .*' + forbidden, 'm').test(md)) bad(`出现了要求里没有的栏目「${forbidden}」`);
}

// ============ 4. 图表二级编号、位置与正文引用 ============
const chapterOf = (idx) => {
  let ch = 0;
  for (let i = 0; i <= idx; i++) {
    const m = /^# (\d+) /.exec(lines[i]);
    if (m) ch = Number(m[1]);
  }
  return ch;
};
const CAP = String.raw`(?:\*\*|<p align="center"><b>)`;
const isCaption = (l) => new RegExp('^' + CAP + '(图|表)(\\d+)-(\\d+)\\s').test(l.trim());
const skip = (i, dir) => {
  i += dir;
  while (i >= 0 && i < lines.length) {
    const t = lines[i].trim();
    if (t !== '') return i;
    i += dir;
  }
  return -1;
};
const last = { 图: {}, 表: {} };
const captions = [];
lines.forEach((l, i) => {
  const t = l.trim();
  if (!isCaption(t)) return;
  const [, kind, ch, seq] = new RegExp('^' + CAP + '(图|表)(\\d+)-(\\d+)\\s').exec(t);
  const num = Number(ch);
  captions.push({ kind, ch: num, seq: Number(seq), text: t, line: i });
  if (!lines.some((o, j) => j > i && /^# \d+ /.test(o))) { /* 图题出现在最后一章之后也要校验 */ }
  // 4a 编号的章号必须等于所在章
  const real = chapterOf(i);
  if (num !== real) bad(`${t.slice(0, 24)} 的章号 ${num} 与所在章 ${real} 不符`);
  // 4b 同章内序号从 1 连续递增
  const key = kind + num, prev = last[key];
  if (prev === undefined) { if (Number(seq) !== 1) bad(`${key} 首项编号应为 ${kind}1-1，实为 ${kind}${num}-${seq}`); }
  else if (Number(seq) !== prev + 1) bad(`${key} 序号不连续：${kind}${num}-${prev} 之后是 ${kind}${num}-${seq}`);
  last[key] = Number(seq);
  // 4c 图题在图下方、表题在表上方
  if (kind === '图') {
    const p = skip(i, -1);
    if (p < 0 || !/^!\[[^\]]*\]\([^)]+\)$/.test(lines[p].trim())) bad(`${kind}${num}-${seq} 的图题不在图片下方`);
  } else {
    const p = skip(i, +1);
    if (p < 0 || !/^\s*\|/.test(lines[p])) bad(`${kind}${num}-${seq} 的表题不在表格上方`);
  }
  // 4d 必须在正文中被引用（模板：所有表格、插图在正文中要有引用）
  const id = kind + num + '-' + seq;
  const refs = lines.filter((o, j) => j !== i && o.includes(id)).length;
  if (refs === 0) bad(`${id} 在正文中没有任何引用（模板禁止孤立图表）`);
});
console.log(`图表编号：图 ${captions.filter(c => c.kind === '图').length} 张 / 表 ${captions.filter(c => c.kind === '表').length} 张`);
// 4e 章节内出现的图片/表格必须都有编号题
lines.forEach((l, i) => {
  if (chapterOf(i) === 0) return;
  const t = l.trim();
  if (/^!\[[^\]]*\]\([^)]+\)$/.test(t)) {
    if (!isCaption(lines[skip(i, +1)] ?? '')) bad(`第 ${i + 1} 行的图片下方没有「图X-Y」图题`);
  }
});
// 反向检查：正文章节里的每一个表格块，上方必须紧跟「表X-Y」表题
for (let i = 0; i < lines.length; i++) {
  if (!/^\s*\|/.test(lines[i])) continue;
  if (i > 0 && /^\s*\|/.test(lines[i - 1])) continue; // 不是表格块的首行
  if (chapterOf(i) === 0) continue;                   // 封面区不参与编号
  const p = skip(i, -1);
  if (p < 0 || !isCaption(lines[p])) bad(`第 ${i + 1} 行的表格上方没有「表X-Y」表题：${lines[i].trim().slice(0, 30)}`);
}
// 只查两种真违规：题名行没用二级编号、"见/如/按"式引用没用二级编号。
// 不做全文扫描，否则「物种表 12 列」这类正常中文会被误判。
for (const [i, l] of lines.entries()) {
  const t = l.trim();
  const capBad = new RegExp('^' + CAP + '([图表]) ?\\d+(?![\\d-])').exec(t);
  if (capBad) bad(`第 ${i + 1} 行题名未用二级编号（模板要求「${capBad[1]}章-序」）：${t.slice(0, 26)}`);
  const refBad = /(见|如|按|归并入)\s*([图表])\s?(\d+)(?![\d-])/.exec(t.replace(/[图表] ?\d+-\d+/g, ''));
  if (refBad) bad(`第 ${i + 1} 行「${refBad[0]}」未用二级编号，模板要求「${refBad[2]}章-序」`);
}

// ============ 4b. 需求分析文档不得残留实现词汇与"原型"字样 ============
const TECH = ['/api/', 'GET ', 'POST ', 'meta.', 'is_public', 'observerStats', 'userTotal', 'pendingUserTotal',
  'ecosystemStats', 'page=0', 'size=100', 'window.print', 'Vue', 'Spring', 'MySQL', 'Leaflet', 'ECharts',
  'X-XSRF-TOKEN', 'SameSite', 'HttpOnly', 'ChartBox', 'GROUP BY', 'left join', '{name, value}', '/dashboard',
  '/stats', '/map', 'N+1', 'layerGroup', 'DOM', 'HTML', 'canEdit', 'canViewStats', 'permitAll', 'hasAnyRole'];
const subOf = (idx) => {
  let s = '';
  for (let i = 0; i <= idx; i++) { const m = /^## (\d+\.\d+)/.exec(lines[i]); if (m) s = m[1]; }
  return s;
};
let exempt = 0;
lines.forEach((l, i) => {
  const sub = subOf(i);
  if (sub === '5.3' || sub === '5.5') { exempt++; return; } // 代码依据列与交付文件清单允许出现标识符
  const hit = TECH.find((t) => l.includes(t));
  if (hit) bad(`第 ${i + 1} 行残留实现词汇「${hit}」（需求分析文档不写实现）：${l.trim().slice(0, 46)}`);
});
for (const [i, l] of lines.entries()) if (l.includes('原型')) bad(`第 ${i + 1} 行出现「原型」，本次实验是需求分析：${l.trim().slice(0, 40)}`);
console.log(`实现词汇扫描：豁免 ${exempt} 行（5.3 依据、5.5 交付清单）`);

// ============ 5. 用例覆盖（期望集合写死，不随文件内容变化）============
const EXPECTED = ['MBIS-VRM-1', 'MBIS-VRM-2', 'MBIS-VRM-3', 'MBIS-VRM-4', 'MBIS-VRM-5', 'MBIS-VRM-6', 'MBIS-VRM-7'];
for (const u of EXPECTED) {
  if (!md.includes(u)) { bad(`期望的用例编号 ${u} 在文中完全不存在`); continue; }
  if (!new RegExp(`\\*\\*用例ID：\\*\\* \\| ${u}`).test(md)) bad(`${u} 缺少「用例ID」行`);
}
const stale = [...new Set([...md.matchAll(/UC4[- ]?\d/g)].map(m => m[0]))];
if (stale.length) bad(`残留旧编号 ${stale.join(' ')}（应与 MBIS-VRM-n 统一）`);

// ============ 6. 参考模板的栏目 ============
const COLS = ['用例名称：', '用例ID：', '角色：', '用例说明：', '前置条件：', '基本事件流：', '其它事件流：', '异常事件流：', '后置条件：'];
const specs = (md.match(/\*\*用例ID：\*\*/g) || []).length;
console.log(`用例规约表数量 = ${specs}（期望 ${EXPECTED.length}）`);
if (specs !== EXPECTED.length) bad(`规约表数量 ${specs} 与用例数 ${EXPECTED.length} 不符`);
for (const c of COLS) {
  const n = md.split(c).length - 1;
  if (n < EXPECTED.length) bad(`栏目「${c}」只出现 ${n} 次，少于用例数 ${EXPECTED.length}`);
}
for (const h of ['| 需求编号 | 数据项 | 数据项含义 | 数据类型 | 取值范围 | 备注 |', '| 规则编号 | 效验规则 |', '| 缩写、术语 | 解 释 |']) {
  if (!md.includes(h)) bad(`缺少模板表头：${h}`);
}
for (const s of ['性能', '可扩展性', '可用性', '安全性', '易用性', '可维护性']) {
  if (!md.includes(s)) bad(`非功能性需求缺少类别「${s}」`);
}
if (!/f-\d+/.test(md)) bad('规则编号未用模板的 f-n 形式');
if (!/FR-VRM-\d/.test(md)) bad('数据字典需求编号未用模板的 FR-模块-n-m 形式');

// ============ 7. 每个功能点都要有活动图（或显式说明归并）============
for (const u of EXPECTED) {
  const fig = /图2-\d/.test(md);
  if (!fig) bad('文中没有「图2-n」形式的活动图编号');
  if (!new RegExp(`MBIS-VRM-\\d[\\s\\S]{0,400}?图2-\\d`).test(md) && !/归并入图2-/.test(md)) {
    bad(`用例 ${u} 找不到对应的活动图编号或归并说明`);
    break;
  }
}

console.log(fail ? `\n自查未通过：${fail} 项` : '\n自查通过：图片与源文件齐备、章骨架符合实验2、图表二级编号与正文引用齐备、模板栏目齐全');
process.exit(fail ? 1 : 0);
