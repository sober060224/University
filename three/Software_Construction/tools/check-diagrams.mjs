// 几何校验：解析每张图的 parent 关系算出绝对坐标，检查溢出、纵向流程、重叠、连线穿节点
// 用法：node tools/check-diagrams.mjs [--dump 图名前缀]
import { readFileSync, readdirSync } from 'node:fs';

const files = readdirSync('diagrams').filter(f => f.endsWith('.drawio') && f !== '模块四-全部图表.drawio');
const dumpArg = process.argv.indexOf('--dump');
const dumpPrefix = dumpArg >= 0 ? process.argv[dumpArg + 1] : null;
let totalFail = 0;

const parseCells = (xml) => {
  const verts = [], edges = [];
  for (const m of xml.matchAll(/<mxCell id="([^"]+)"(?: value="([^"]*)")?[^>]*?vertex="1" parent="([^"]*)"[^>]*?><mxGeometry x="(-?\d+)" y="(-?\d+)" width="(\d+)" height="(\d+)"/g)) {
    verts.push({ id: m[1], value: (m[2] ?? '').replace(/&#xa;/g, '\n'), parent: m[3], x: +m[4], y: +m[5], w: +m[6], h: +m[7] });
  }
  for (const m of xml.matchAll(/<mxCell id="([^"]+)"[^>]*?edge="1"[^>]*?source="([^"]+)" target="([^"]+)"/g)) {
    edges.push({ id: m[1], from: m[2], to: m[3] });
  }
  return { verts, edges };
};

for (const f of files) {
  const xml = readFileSync('diagrams/' + f, 'utf8');
  const svg = readFileSync('diagrams/' + f.replace('.drawio', '.svg'), 'utf8');
  const canvasW = Number(/<svg[^>]*\swidth="(\d+)"/.exec(svg)[1]);
  const canvasH = Number(/<svg[^>]*\sheight="(\d+)"/.exec(svg)[1]);
  const { verts, edges } = parseCells(xml);
  const isActivity = /^AD-/.test(f);

  const byId = Object.fromEntries(verts.map(v => [v.id, v]));
  const abs = (v) => {
    const o = v.parent === '1' || v.parent === 'bnd' || v.parent.startsWith('lane') ? (byId[v.parent] ?? { x: 0, y: 0 }) : { x: 0, y: 0 };
    return { id: v.id, x: o.x + v.x, y: o.y + v.y, w: v.w, h: v.h };
  };
  const boxes = Object.fromEntries(verts.map(v => [v.id, abs(v)]));

  const issues = [];
  // 1) 溢出画布 / 泳道 / 系统边界
  for (const v of verts) {
    const b = boxes[v.id];
    const l = b.x, t = b.y, r = l + b.w, bo = t + b.h;
    if (l < 0 || t < 0 || r > canvasW || bo > canvasH)
      issues.push(`${v.id} 超出画布(${canvasW}x${canvasH}) → 横 ${l}..${r} 纵 ${t}..${bo}`);
    if (v.parent.startsWith('lane')) {
      const lane = byId[v.parent];
      if (bo > lane.y + lane.h) issues.push(`${v.id} 下方溢出泳道 ${v.parent}（元素底 ${bo} > 泳道底 ${lane.y + lane.h}）`);
      if (r > lane.x + lane.w) issues.push(`${v.id} 右方溢出泳道 ${v.parent}（元素右 ${r} > 泳道右 ${lane.x + lane.w}）`);
    }
    if (v.parent === 'bnd') {
      const bd = byId['bnd'];
      if (r > bd.x + bd.w) issues.push(`${v.id} 右方溢出系统边界（元素右 ${r} > 边界右 ${bd.x + bd.w}）`);
      if (bo > bd.y + bd.h) issues.push(`${v.id} 下方溢出系统边界（元素底 ${bo} > 边界底 ${bd.y + bd.h}）`);
      if (l < bd.x) issues.push(`${v.id} 左方溢出系统边界`);
    }
  }

  if (isActivity) {
    // 2) 泳道必须是竖列（纵向流程的前提）
    for (const v of verts.filter(v => v.id.startsWith('lane'))) {
      if (v.h <= v.w) issues.push(`${v.id} 不是竖列泳道（高 ${v.h} ≤ 宽 ${v.w}），流程会是水平分布`);
    }
    // 2b) 分区必须恰为「用户」「系统」——需求分析的活动图不分前端/后端/数据库
    const laneNames = verts.filter(v => v.id.startsWith('lane')).map(v => v.value);
    if (laneNames.join('|') !== '用户|系统') issues.push(`分区应为「用户」「系统」两栏，实为「${laneNames.join('」「')}」`);
    // 2c) 节点措辞不得含实现词汇（接口、框架、SQL、键名等属于设计阶段）
    const TECH = ['/api/', 'GET ', 'POST ', 'meta.', 'GROUP BY', 'left join', 'window.print', 'Vue', 'Spring',
      'MySQL', 'Leaflet', 'ECharts', 'layerGroup', '.xlsx', 'is_public', 'observerStats', 'page=', 'size=', '{name', 'DOM', 'HTML', 'Top10', 'alert', 'canEdit', 'canView'];
    for (const v of verts.filter(v => !v.id.startsWith('lane'))) {
      const hit = TECH.find((t) => v.value.includes(t));
      if (hit) issues.push(`${v.id} 的标签含实现词汇「${hit}」：${v.value.replace(/\n/g, ' ')}`);
    }
    // 3) 每条边必须严格自上而下
    for (const e of edges) {
      const s = boxes[e.from], t = boxes[e.to];
      if (!s || !t) { issues.push(`边 ${e.id} 端点缺失：${e.from}→${e.to}`); continue; }
      if (t.y <= s.y) issues.push(`边 ${e.from}→${e.to} 不是向下：源 y=${s.y} 目标 y=${t.y}`);
    }
    // 4) 起点全图最上、终点全图最下
    const ys = verts.filter(v => !v.id.startsWith('lane')).map(v => boxes[v.id].y);
    const minY = Math.min(...ys), maxY = Math.max(...ys);
    for (const v of verts.filter(v => v.id === 's' || v.id === 'start')) if (boxes[v.id].y !== minY) issues.push(`起点 ${v.id} 不在最上层（y=${boxes[v.id].y}，最小 ${minY}）`);
    for (const v of verts.filter(v => v.id === 'e' || v.id === 'end')) if (boxes[v.id].y !== maxY) issues.push(`终点 ${v.id} 不在最下层（y=${boxes[v.id].y}，最大 ${maxY}）`);
    // 5) flow 必须无环
    const adj = {};
    edges.forEach(e => (adj[e.from] ||= []).push(e.to));
    const seen = {}, stack = {};
    const dfs = (u) => {
      if (stack[u]) { issues.push(`flow 存在环，经过 ${u}`); return; }
      if (seen[u]) return;
      seen[u] = stack[u] = 1;
      (adj[u] || []).forEach(dfs);
      stack[u] = 0;
    };
    Object.keys(adj).forEach(dfs);
    // 6) 节点包围盒不得互相重叠
    const bs = verts.filter(v => !v.id.startsWith('lane')).map(v => boxes[v.id]);
    for (let i = 0; i < bs.length; i++) for (let j = i + 1; j < bs.length; j++) {
      const a = bs[i], b = bs[j];
      if (a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h)
        issues.push(`节点 ${a.id} 与 ${b.id} 重叠`);
    }
    // 7) SVG 折线与 drawio 边必须同源同序，且不得穿过第三方节点
    const polys = [...svg.matchAll(/<polyline points="([^"]+)"/g)].map(m => m[1].split(' ').map(p => p.split(',').map(Number)));
    if (polys.length !== edges.length) issues.push(`SVG 折线 ${polys.length} 条 ≠ drawio 边 ${edges.length} 条，两个渲染器已分叉`);
    const hitNode = (seg, exclude) => bs.filter(b => !exclude.includes(b.id))
      .find(b => seg.x1 < b.x + b.w && seg.x2 > b.x && seg.y1 < b.y + b.h && seg.y2 > b.y);
    polys.forEach((pts, i) => {
      const e = edges[i];
      if (!e) return;
      const head = boxes[e.from], tail = boxes[e.to];
      const near = (p, q) => Math.abs(p[0] - q[0]) <= 1 && Math.abs(p[1] - q[1]) <= 1;
      if (head && !near(pts[0], [head.x + head.w / 2, head.y + head.h])) issues.push(`边 ${e.from}→${e.to} 的 SVG 折线起点不在源节点底边 → SVG 与 drawio 走线不一致`);
      if (tail && !near(pts[pts.length - 1], [tail.x + tail.w / 2, tail.y])) issues.push(`边 ${e.from}→${e.to} 的 SVG 折线终点不在目标顶边 → SVG 与 drawio 走线不一致`);
      for (let k = 0; k + 1 < pts.length; k++) {
        const [p, q] = [pts[k], pts[k + 1]];
        const crossed = hitNode({ x1: p[0], y1: p[1], x2: q[0], y2: q[1] }, [e.from, e.to]);
        if (crossed) issues.push(`边 ${e.from}→${e.to} 的连线穿过节点 ${crossed.id}`);
      }
    });
  }

  totalFail += issues.length;
  console.log(`${f}  顶点 ${verts.length} / 边 ${edges.length}，画布 ${canvasW}x${canvasH}：${issues.length ? '有问题' : '通过'}`);
  issues.forEach(i => console.log('   ✗ ' + i));

  if (dumpPrefix && f.startsWith(dumpPrefix)) {
    console.log('   —— 坐标（绝对）——');
    verts.filter(v => !v.id.startsWith('lane')).forEach(v => {
      const b = boxes[v.id];
      console.log(`   ${v.id.padEnd(4)} lane=${v.parent.padEnd(6)} x=${String(b.x).padStart(4)} y=${String(b.y).padStart(5)} w=${String(b.w).padStart(3)} h=${String(b.h).padStart(3)}`);
    });
  }
}
console.log(totalFail ? `\n共 ${totalFail} 处几何问题` : '\n全部通过：无溢出、泳道为竖列、所有连线自上而下、无环、无重叠、无连线穿节点');
process.exit(totalFail ? 1 : 0);
