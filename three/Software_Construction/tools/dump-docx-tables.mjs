// 只读抽取 .docx 的表格栏目：node tools/dump-docx-tables.mjs <文件.docx> [关键词]
import { execFileSync } from 'node:child_process';

const file = process.argv[2];
const kw = process.argv[3];
const xml = execFileSync('unzip', ['-p', file, 'word/document.xml'], { maxBuffer: 1 << 26 }).toString('utf8');
const tbls = [...xml.matchAll(/<w:tbl>[\s\S]*?<\/w:tbl>/g)].map(m => m[0]);
console.log(`表格总数 = ${tbls.length}`);

const rowText = (t) => [...t.matchAll(/<w:tr[\s\S]*?<\/w:tr>/g)].map(r =>
  [...r[0].matchAll(/<w:tc[\s\S]*?<\/w:tc>/g)].map(c =>
    [...c[0].matchAll(/<w:t(?:\s[^>]*)?>([\s\S]*?)<\/w:t>/g)].map(x => x[1]).join('')
  ).join(' | '));

tbls.forEach((t, i) => {
  const rows = rowText(t);
  if (kw && !rows.some(r => r.includes(kw))) return;
  console.log(`\n──── 表 ${i}（${rows.length} 行）────`);
  rows.slice(0, 12).forEach(r => console.log('  ' + r.slice(0, 200)));
  if (rows.length > 12) console.log(`  …其余 ${rows.length - 12} 行`);
});
