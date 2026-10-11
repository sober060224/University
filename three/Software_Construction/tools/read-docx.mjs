// 只读解析 .docx：按文档顺序输出段落与表格，用于查看参考模板的栏目结构
import { readFileSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';

const file = process.argv[2];
const xml = execFileSync('unzip', ['-p', file, 'word/document.xml'], { maxBuffer: 64 * 1024 * 1024 }).toString('utf8');

const decode = (s) => s
  .replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&amp;/g, '&')
  .replace(/&#(\d+);/g, (_, n) => String.fromCharCode(+n));

// 顶层块级元素按顺序扫描：w:p 与 w:tbl
const out = [];
let i = 0;
const bodyStart = xml.indexOf('<w:body>');
let cursor = bodyStart + 6;

function readParagraph(chunk) {
  const texts = [...chunk.matchAll(/<w:t(?:\s[^>]*)?>([\s\S]*?)<\/w:t>/g)].map(m => decode(m[1]));
  const style = /<w:pStyle w:val="([^"]+)"/.exec(chunk);
  const numPr = /<w:numId w:val="([^"]+)"/.test(chunk);
  return { text: texts.join(''), style: style ? style[1] : '', list: numPr };
}
function readTable(chunk) {
  const rows = [];
  for (const rm of chunk.matchAll(/<w:tr(?:\s[^>]*)?>([\s\S]*?)<\/w:tr>/g)) {
    const cells = [];
    for (const cm of rm[1].matchAll(/<w:tc(?:\s[^>]*)?>([\s\S]*?)<\/w:tc>/g)) {
      const t = [...cm[1].matchAll(/<w:t(?:\s[^>]*)?>([\s\S]*?)<\/w:t>/g)].map(m => decode(m[1])).join('');
      cells.push(t.trim());
    }
    rows.push(cells);
  }
  return rows;
}

// 顺序遍历：找每个 <w:p 或 <w:tbl 的起点
const blockRe = /<(w:p|w:tbl)[ >]/g;
let m;
const blocks = [];
while ((m = blockRe.exec(xml))) blocks.push({ type: m[1], start: m.index });
// 去掉嵌套在 tbl 内部的 p（表格单元格里的段落）
const tblRanges = [...xml.matchAll(/<w:tbl>/g)].map(mm => {
  const endTag = '</w:tbl>';
  let depth = 0, idx = mm.index;
  while (true) {
    const nT = xml.indexOf('<w:tbl>', idx + 1);
    const nE = xml.indexOf(endTag, idx + 1);
    if (nE === -1) break;
    if (nT !== -1 && nT < nE) { idx = nT; depth++; } else { idx = nE; depth--; if (depth === 0) return [mm.index, nE + endTag.length]; }
  }
  return [mm.index, -1];
});
const insideTbl = (pos) => tblRanges.some(([s, e]) => pos >= s && pos <= e);

for (const b of blocks) {
  if (b.type === 'w:p' && insideTbl(b.start)) continue;
  if (b.type === 'w:tbl') {
    const rng = tblRanges.find(([s]) => s === b.start);
    if (!rng) continue;
    const rows = readTable(xml.slice(rng[0], rng[1]));
    out.push('【表格 ' + rows.length + ' 行】');
    rows.slice(0, 14).forEach(r => out.push('  | ' + r.join(' | ')));
    if (rows.length > 14) out.push(`  …（共 ${rows.length} 行）`);
    continue;
  }
  const end = xml.indexOf('</w:p>', b.start);
  if (end === -1) continue;
  const p = readParagraph(xml.slice(b.start, end));
  if (!p.text.trim()) continue;
  const tag = p.style && p.style.toLowerCase().includes('heading') ? `[${p.style}] ` : (p.style ? `[${p.style}] ` : '');
  out.push(tag + p.text);
}

console.log(out.join('\n'));
