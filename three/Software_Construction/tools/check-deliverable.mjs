// 交付前自查：图表齐备与引用完整、模板栏目、角色命名，以及"不写实现细节"这条边界
// 用法：node tools/check-deliverable.mjs lab02/需求规范说明书-模块四-数据可视化与报表.md
import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const MD = process.argv.slice(2).find((a) => !a.startsWith('--'));
if (!MD) { console.error('用法：node tools/check-deliverable.mjs <报告.md> [--lab=2|3]'); process.exit(1); }
const BASE = dirname(resolve(MD));            // 图片链接是相对 md 所在目录的，不是相对仓库根
// diagrams 可能在 md 同级或上一级（lab02/ 用 ../diagrams）
let DIR = BASE;
while (DIR !== dirname(DIR) && !existsSync(resolve(DIR, 'diagrams'))) DIR = dirname(DIR);
const DG = resolve(DIR, 'diagrams');
if (!existsSync(DG)) { console.error('找不到 diagrams 目录，无法核对产物'); process.exit(1); }
const md = readFileSync(MD, 'utf8');
const lines = md.split(/\r?\n/);
// 章骨架按实验号选择：实验2（需求规范说明书）为默认，实验3（架构设计报告）用 --lab=3
const LAB = ((process.argv.find((a) => a.startsWith('--lab=')) || '').split('=')[1]) || '2';
let fail = 0, warn = 0;
const bad = (m) => { console.log('   ✗ ' + m); fail++; };
const note = (m) => { console.log('   ! ' + m); warn++; };

// 单元格级归一化：把「| a   | b     |」压成「| a | b |」，容忍表格对齐用的多余空格
const norm = (s) => s.replace(/\s+/g, ' ').trim();

// ============ 1. 图与源文件是否齐备（期望集合取自磁盘产物）============
const svgs = readdirSync(DG).filter(f => f.endsWith('.svg')).sort();
for (const f of svgs) if (!md.includes('diagrams/' + f)) bad(`报告未嵌入 ${f}`);
for (const m of md.matchAll(/!\[[^\]]*\]\(([^)]+)\)/g)) {
  if (!existsSync(resolve(BASE, m[1]))) bad(`正文链接的图片解析不到实际文件：${m[1]}`);
}
const pairs = readdirSync(DG).filter(f => f.endsWith('.drawio') && f !== '模块四-全部图表.drawio');
const orphan = pairs.filter(f => !existsSync(resolve(DG, f.replace('.drawio', '.svg'))));
if (orphan.length) bad(`有 .drawio 没有同名 .svg：${orphan.join(', ')}`);
console.log(`图：磁盘 ${svgs.length} 张 svg / ${pairs.length} 份 drawio 源，正文嵌入 ${(md.match(/!\[[^\]]*\]\(/g) || []).length} 处`);
if (!md.includes('.drawio')) note('正文没有列出 .drawio 可编辑源文件的路径（源文件本身仍在 diagrams/ 里随仓库提交）');

// ============ 2. 表格列数一致 ============
let cols = -1;
const colBad = [];
lines.forEach((l, i) => {
  if (/^\s*\|.*\|\s*$/.test(l)) {
    const n = (l.match(/\|/g) || []).length;
    if (cols < 0) cols = n;
    else if (n !== cols) colBad.push(`${i + 1} 行：${n} vs 表头 ${cols}`);
  } else if (cols >= 0 && l.trim() !== '') cols = -1;
});
console.log(`表格列数：${colBad.length ? colBad.length + ' 行不一致' : '全部一致'}`);
colBad.slice(0, 8).forEach(b => bad(b));

// ============ 3. 章骨架（按 --lab 选择；缺省实验2，行为与以前一致）============
const SKELETON = {
  2: [
    ['# 1 系统缩写表', '系统缩写表'],
    ['# 2 核心功能需求', '核心功能需求（用例图 + 用例规约 + 活动图）'],
    ['# 3 非功能性需求', '非功能性需求'],
    ['# 4 数据字典与规则表', '数据字典与规则表'],
    ['# 5 其他必要的需求分析内容', '其他必要的需求分析内容'],
  ],
  3: [
    ['# 1 模块四架构概述', '模块四架构概述'],
    ['# 2 模块四的类汇总表', '模块四的类汇总表'],
    ['# 3 详细包图与包中的类', '详细包图与包中的类'],
    ['# 4 调用关系图', '调用关系图'],
  ],
};
if (!SKELETON[LAB]) { console.error(`未知实验号 --lab=${LAB}`); process.exit(1); }
for (const [re, name] of SKELETON[LAB]) if (!lines.some(l => l.startsWith(re))) bad(`缺少实验${LAB} 要求的章节：${name}（应有一级标题「${re}」）`);
for (const forbidden of ['参考文献', '变更记录', '待确认', '已知限制', '交付图清单']) {
  if (new RegExp('^#{1,3} .*' + forbidden, 'm').test(md)) bad(`出现了已决定不要的栏目「${forbidden}」`);
}

// ============ 4. 图表二级编号、题名位置、交叉引用完整 ============
const CAP = String.raw`(?:\*\*|<p align="center"><b>)`;
const capRe = () => new RegExp('^' + CAP + '(图|表)(\\d+)-(\\d+)\\s');
const isCaption = (l) => capRe().test(norm(l).replace(/^\| /, ''));
const chapterOf = (idx) => { let ch = 0; for (let i = 0; i <= idx; i++) { const m = /^# (\d+) /.exec(lines[i]); if (m) ch = Number(m[1]); } return ch; };
const skip = (i, dir) => {
  i += dir;
  while (i >= 0 && i < lines.length) { if (lines[i].trim() !== '') return i; i += dir; }
  return -1;
};
const seen = { 图: {}, 表: {} }, titled = new Set();
lines.forEach((l, i) => {
  const t = norm(l);
  const m = capRe().exec(t);
  if (!m) return;
  const kind = m[1], num = Number(m[2]), seq = Number(m[3]);
  titled.add(kind + num + '-' + seq);
  const real = chapterOf(i);
  if (num !== real) bad(`${kind}${num}-${seq} 的章号与所在章 ${real} 不符`);
  const prev = seen[kind][num];
  if (prev === undefined) { if (seq !== 1) bad(`${kind}${num} 首项应为 ${kind}${num}-1，实为 -${seq}`); }
  else if (seq !== prev + 1) bad(`${kind}${num} 序号不连续：-${prev} 之后是 -${seq}`);
  seen[kind][num] = seq;
  if (kind === '图') {
    const p = skip(i, -1);
    if (p < 0 || !/^!\[[^\]]*\]\([^)]+\)$/.test(lines[p].trim())) bad(`${kind}${num}-${seq} 的图题不在图片下方`);
  } else {
    const p = skip(i, +1);
    if (p < 0 || !/^\s*\|/.test(lines[p])) bad(`${kind}${num}-${seq} 的表题不在表格上方`);
  }
  const refs = lines.filter((o, j) => j !== i && o.includes(kind + num + '-' + seq)).length;
  if (refs === 0) bad(`${kind}${num}-${seq} 在正文中没有任何引用（模板禁止孤立图表）`);
});
console.log(`图表编号：图 ${Object.keys(seen['图'] || {}).length} 章 / 共 ${(md.match(new RegExp(CAP + '图\\d+-\\d+', 'g')) || []).length} 张，表 ${(md.match(new RegExp(CAP + '表\\d+-\\d+', 'g')) || []).length} 张`);
// 正文提到的图/表编号必须真的有题名（防悬空引用）。只看「见/如/按」这类引用动词，
// 否则引用别处文档的图表（如 lab01 的表5-1）会被误报
for (const m of md.matchAll(/(?:见|如|按)\s*([图表])(\d+-\d+)/g)) {
  if (!titled.has(m[1] + m[2])) bad(`正文引用了不存在的 ${m[1]}${m[2]}`);
}
// 反向：正文章节里每个表格块上方要有表题
for (let i = 0; i < lines.length; i++) {
  if (!/^\s*\|/.test(lines[i])) continue;
  if (i > 0 && /^\s*\|/.test(lines[i - 1])) continue;
  if (chapterOf(i) === 0) continue;
  const p = skip(i, -1);
  if (p < 0 || !isCaption(lines[p])) bad(`第 ${i + 1} 行的表格上方没有「表X-Y」表题：${norm(lines[i]).slice(0, 30)}`);
}
lines.forEach((l, i) => {
  if (chapterOf(i) === 0) return;
  const t = l.trim();
  if (/^!\[[^\]]*\]\([^)]+\)$/.test(t) && !isCaption(lines[skip(i, +1)] ?? '')) bad(`第 ${i + 1} 行的图片下方没有「图X-Y」图题`);
});

// ============ 5–6. 以下两类检查只对实验2 的《需求规范说明书》成立 ============
// 架构设计报告本来就要写包名、类名、接口路径，所以实验3 不做「不写实现细节」扫描，也不查用例规约表。
if (LAB === '2') {
// ============ 5. 不写实现细节、不写"原型"，角色只用中文名 ============
const TECH = ['/api/', 'GET ', 'POST ', 'meta.', 'is_public', 'observerStats', 'userTotal', 'pendingUserTotal',
  'ecosystemStats', 'page=0', 'size=100', 'window.print', 'Vue', 'Spring', 'MySQL', 'Leaflet', 'ECharts',
  'X-XSRF-TOKEN', 'SameSite', 'HttpOnly', 'ChartBox', 'GROUP BY', 'left join', '{name, value}', '/dashboard',
  '/stats', '/map', 'N+1', 'layerGroup', 'DOM', 'HTML', 'canEdit', 'canViewStats', 'permitAll', 'hasAnyRole',
  'RESEARCHER', 'STUDENT', 'PENDING', 'SecurityConfig', 'router/index'];
// 英文角色名单独列：正文一律禁止（不再有任何豁免小节）
const ROLES_EN = ['Admin', 'Researcher', 'Student', 'Public', 'ADMIN', '系统管理员', '科研人员'];
let hit = 0;
lines.forEach((l, i) => {
  const t = TECH.find((x) => l.includes(x));
  if (t) { bad(`第 ${i + 1} 行残留实现词汇「${t}」：${norm(l).slice(0, 40)}`); hit++; }
  const r = ROLES_EN.find((x) => l.includes(x));
  if (r && !l.includes('学生姓名')) bad(`第 ${i + 1} 行用了旧角色称谓「${r}」，应改用 游客／用户／数据审核员／管理员`);
});
if (md.includes('原型')) bad('出现「原型」，本实验是需求分析');
for (const r of ['游客', '数据审核员', '管理员', '用户']) {
  if (!md.includes(r)) bad(`角色集合缺少「${r}」`);
}
console.log(`实现词汇与角色命名扫描：${hit} 处实现词命中，英文旧角色名 ${lines.filter(l => ROLES_EN.some(r => l.includes(r))).length} 行`);

// ============ 6. 用例覆盖与模板栏目（期望集合写死）============
const EXPECTED = ['MBIS-VRM-1', 'MBIS-VRM-2', 'MBIS-VRM-3', 'MBIS-VRM-4', 'MBIS-VRM-5', 'MBIS-VRM-6', 'MBIS-VRM-7'];
for (const u of EXPECTED) {
  if (!md.includes(u)) { bad(`期望的用例编号 ${u} 在文中不存在`); continue; }
  if (!new RegExp('\\*\\*用例ID：\\*\\*\\s*\\|\\s*' + u).test(norm(md))) bad(`${u} 缺少「用例ID」行`);
}
const stale = [...new Set([...md.matchAll(/UC4[-_]?\d/g)].map(m => m[0]))];
if (stale.length) bad(`残留旧编号 ${stale.join(' ')}`);
const COLS = ['用例名称：', '用例ID：', '角色：', '用例说明：', '前置条件：', '基本事件流：', '其它事件流：', '异常事件流：', '后置条件：'];
const specs = (md.match(/\*\*用例ID：\*\*/g) || []).length;
console.log(`用例规约表数量 = ${specs}（期望 ${EXPECTED.length}）`);
if (specs !== EXPECTED.length) bad(`规约表数量 ${specs} ≠ 用例数 ${EXPECTED.length}`);
for (const c of COLS) { const n = md.split(c).length - 1; if (n < EXPECTED.length) bad(`栏目「${c}」只出现 ${n} 次`); }
for (const h of ['需求编号 | 数据项 | 数据项含义 | 数据类型 | 取值范围 | 备注', '规则编号 | 效验规则', '缩写、术语 | 解 释']) {
  if (!norm(md).includes(h)) bad(`缺少模板表头：${h}`);
}
for (const s of ['性能', '可扩展性', '可用性', '安全性', '易用性', '可维护性']) if (!md.includes(s)) bad(`NFR 缺类别「${s}」`);
if (!/f-\d+/.test(md)) bad('规则编号未用 f-n 形式');
if (!/FR-VRM-\d/.test(md)) bad('数据字典需求编号未用 FR-模块-n-m 形式');
}

console.log(fail ? `\n自查未通过：${fail} 项（另有 ${warn} 条提示）` : `\n自查通过：图表编号与引用完整、` + (LAB === '2' ? '章骨架符合实验2、角色已改为中文四档、无实现词汇' : '章骨架符合实验3（架构设计）') + `（${warn} 条提示）`);
process.exit(fail ? 1 : 0);
