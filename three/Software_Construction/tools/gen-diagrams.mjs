// 模块四需求分析的图源生成器：一份规格 → .drawio（可编辑源）+ .svg（嵌入报告）
// 只为生成 diagrams/ 服务，不参与前后端构建。用法：node tools/gen-diagrams.mjs
import { writeFileSync, mkdirSync } from 'node:fs';

const OUT = 'diagrams';
mkdirSync(OUT, { recursive: true });

const COL = 200, ROW = 120, OX = 40, OY = 40;
const x = (c) => c * COL + OX;
const y = (r) => r * ROW + OY;
// 活动图分区只按需求职责分栏：事件流的文本本身就是「用户做… / 系统给…」两股交替，
// 前端/后端/数据库属于设计阶段，不写进需求分析的活动图。
const LANES = ['用户', '系统'];
const LANE_COLOR = ['#f5f5f5', '#e8f4f8'];

// ============ 1. 用例图 ============
const USECASE = {
  file: 'MBIS-VRM-用例图',
  title: '模块四 数据可视化与报表 — 用例图',
  kind: 'usecase',
  boundary: { x: 340, y: 20, w: 940, h: 1220, label: '海洋生物多样性信息管理系统（模块四）' },
  nodes: [
    { id: 'A_ADMIN', col: 0, row: 1, shape: 'actor', label: '系统管理员' },
    { id: 'A_RES', col: 0, row: 5, shape: 'actor', label: '科研人员' },
    { id: 'A_STU', col: 0, row: 8, shape: 'actor', label: '学生' },
    { id: 'A_PUB', col: 0, row: 10, shape: 'actor', label: '公众' },
    { id: 'UC01', col: 3, row: 1, label: 'MBIS-VRM-1\n查看综合数据看板' },
    { id: 'UC02', col: 3, row: 3, label: 'MBIS-VRM-2\n查看统计分析图表' },
    { id: 'UC03', col: 5, row: 2, label: 'MBIS-VRM-3\n切换统计维度页签' },
    { id: 'UC04', col: 3, row: 5, label: 'MBIS-VRM-4\n查看物种分布地图' },
    { id: 'UC05', col: 3, row: 6, label: 'MBIS-VRM-5\n查看观测地点地图' },
    { id: 'UC06', col: 3, row: 8, label: 'MBIS-VRM-6\n导出 Excel 报表' },
    { id: 'UC07', col: 3, row: 9, label: 'MBIS-VRM-7\n打印 / 导出 PDF' },
    { id: 'UC08', col: 5, row: 4, label: '获取统计数据' },
    { id: 'UC09', col: 5, row: 6, label: '显示地图底图' },
    { id: 'UC10', col: 5, row: 8, label: '获取全量业务数据' },
  ],
  edges: [
    { from: 'A_RES', to: 'UC01' }, { from: 'A_RES', to: 'UC02' }, { from: 'A_RES', to: 'UC04' },
    { from: 'A_RES', to: 'UC05' }, { from: 'A_RES', to: 'UC06' }, { from: 'A_RES', to: 'UC07' },
    { from: 'A_ADMIN', to: 'A_RES', style: 'general' },
    { from: 'A_STU', to: 'UC01' }, { from: 'A_STU', to: 'UC02' }, { from: 'A_STU', to: 'UC04' },
    { from: 'A_STU', to: 'UC05' }, { from: 'A_PUB', to: 'UC04' },
    { from: 'UC01', to: 'UC08', style: 'include' }, { from: 'UC02', to: 'UC08', style: 'include' },
    { from: 'UC03', to: 'UC02', style: 'extend' },
    { from: 'UC04', to: 'UC09', style: 'include' }, { from: 'UC05', to: 'UC09', style: 'include' },
    { from: 'UC06', to: 'UC10', style: 'include' },
  ],
};

// ============ 2~7. 活动图（分区：用户 / 系统；措辞：需求语言，不写实现）============
function act(file, title, rows, flow) {
  return { file, title, kind: 'activity', lanes: LANES, rows, flow };
}
const ACTS = [
  act('AD-MBIS-VRM-1-查看综合数据看板', 'MBIS-VRM-1 查看综合数据看板 — 活动图', [
    ['0', 0, 's', 'start', ''],
    ['0', 1, 'a1', 'activity', '打开\n综合数据看板'],
    ['1', 2, 'a2', 'activity', '校验该角色\n是否有权查看统计'],
    ['1', 3, 'd1', 'decision', '有权查看\n统计?'],
    ['1', 4, 'rj', 'activity', '拒绝进入，转到\n该角色可见页面'],
    ['1', 5, 'a3', 'activity', '按访问者可见范围\n汇总概览与分布统计'],
    ['1', 6, 'd2', 'decision', '可了解他人工作量\n与账号排队情况?'],
    ['1', 7, 'a4', 'activity', '一并给出'],
    ['1', 8, 'a5', 'activity', '不予呈现'],
    ['1', 9, 'a6', 'activity', '呈现概览指标\n与分布图'],
    ['0', 10, 'e', 'end', ''],
    ['-1', 4, 'n1', 'note', '公众角色无此入口，直接索取也被拒绝'],
  ], [
    ['s', 'a1'], ['a1', 'a2'], ['a2', 'd1'], ['d1', 'rj', '否'], ['d1', 'a3', '是'],
    ['rj', 'e'], ['a3', 'd2'], ['d2', 'a4', '是'], ['a4', 'a6'], ['d2', 'a5', '否'],
    ['a5', 'a6'], ['a6', 'e'],
  ]),
  act('AD-MBIS-VRM-2-查看统计分析图表', 'MBIS-VRM-2 查看统计分析图表（含 MBIS-VRM-3 切换页签）— 活动图', [
    ['0', 0, 's', 'start', ''],
    ['0', 1, 'a1', 'activity', '进入\n统计分析'],
    ['0', 5, 'a5', 'activity', '切换到\n观测维度'],
    ['0', 9, 'e', 'end', ''],
    ['1', 2, 'a2', 'activity', '校验统计查看权'],
    ['1', 3, 'a3', 'activity', '汇总各维度统计'],
    ['1', 4, 'a4', 'activity', '呈现\n物种维度图表'],
    ['1', 6, 'd1', 'decision', '结果含观测人员\n活跃度?'],
    ['1', 7, 'a6', 'activity', '一并呈现\n活跃度图表'],
    ['1', 8, 'a7', 'activity', '仅呈现\n其余观测图表'],
  ], [
    ['s', 'a1'], ['a1', 'a2'], ['a2', 'a3'], ['a3', 'a4'], ['a4', 'a5'],
    ['a5', 'd1'], ['d1', 'a6', '是'], ['d1', 'a7', '否'], ['a6', 'e'], ['a7', 'e'],
  ]),
  act('AD-MBIS-VRM-4-查看物种分布地图', 'MBIS-VRM-4 查看物种分布地图 — 活动图', [
    ['0', 0, 's', 'start', ''],
    ['0', 1, 'a1', 'activity', '进入\n分布地图'],
    ['0', 6, 'a7', 'activity', '点选某个分布点'],
    ['0', 8, 'e', 'end', ''],
    ['1', 2, 'a2', 'activity', '显示地图底图'],
    ['1', 3, 'a3', 'activity', '取出已公开且\n登记位置的物种'],
    ['1', 4, 'a4', 'activity', '按保护等级区分颜色\n并生成图例'],
    ['1', 7, 'a5', 'activity', '显示该物种的\n保护信息'],
    ['-1', 4, 'n1', 'note', '图例只列出数据中实际出现的等级'],
  ], [
    ['s', 'a1'], ['a1', 'a2'], ['a2', 'a3'], ['a3', 'a4'], ['a4', 'a7'],
    ['a7', 'a5'], ['a5', 'e'],
  ]),
  act('AD-MBIS-VRM-5-查看观测地点地图', 'MBIS-VRM-5 查看观测地点地图 — 活动图', [
    ['0', 0, 's', 'start', ''],
    ['0', 2, 'a1', 'activity', '切换到\n观测地点图层'],
    ['0', 6, 'a5', 'activity', '点选某个观测点'],
    ['0', 8, 'e', 'end', ''],
    ['1', 1, 'd0', 'decision', '该角色有权\n查看观测数据?'],
    ['1', 1, 'hj', 'activity', '该入口不开放'],
    ['1', 3, 'a2', 'activity', '撤下上一图层'],
    ['1', 4, 'a3', 'activity', '取出可见范围内的\n观测点'],
    ['1', 5, 'a4', 'activity', '按所属生态系统区分\n颜色并生成图例'],
    ['1', 7, 'a6', 'activity', '显示地点、所属生态\n系统与观测时间'],
  ], [
    ['s', 'd0'], ['d0', 'hj', '否'], ['d0', 'a1', '是'], ['hj', 'e'], ['a1', 'a2'],
    ['a2', 'a3'], ['a3', 'a4'], ['a4', 'a5'], ['a5', 'a6'], ['a6', 'e'],
  ]),
  act('AD-MBIS-VRM-6-导出Excel报表', 'MBIS-VRM-6 导出 Excel 报表 — 活动图', [
    ['0', 0, 's', 'start', ''],
    ['0', 2, 'a1', 'activity', '选择导出\n物种／观测报表'],
    ['0', 9, 'e', 'end', ''],
    ['1', 1, 'd1', 'decision', '该角色有权\n导出报表?'],
    ['1', 1, 'hj', 'activity', '导出入口不开放'],
    ['1', 3, 'a2', 'activity', '取出第一批次数据'],
    ['1', 4, 'd2', 'decision', '数据尚未取完?'],
    ['1', 5, 'a3', 'activity', '继续取下一批次\n并合并'],
    ['1', 6, 'a4', 'activity', '整理列并生成\n文件供下载'],
    ['1', 7, 'd3', 'decision', '取数或生成失败?'],
    ['1', 8, 'a5', 'activity', '中止导出\n并说明原因'],
  ], [
    ['s', 'd1'], ['d1', 'hj', '否'], ['d1', 'a1', '是'], ['hj', 'e'], ['a1', 'a2'], ['a2', 'd2'],
    ['d2', 'a3', '是'], ['d2', 'a4', '否'], ['a3', 'a4'], ['a4', 'd3'],
    ['d3', 'a5', '是'], ['a5', 'e'], ['d3', 'e', '否'],
  ]),
  act('AD-MBIS-VRM-7-打印导出PDF', 'MBIS-VRM-7 打印 / 导出 PDF — 活动图', [
    ['0', 0, 's', 'start', ''],
    ['0', 2, 'a1', 'activity', '选择打印\n当前报表'],
    ['0', 4, 'a4', 'activity', '将打印结果\n另存为文档'],
    ['0', 5, 'e', 'end', ''],
    ['1', 1, 'd1', 'decision', '该角色有权\n打印报表?'],
    ['1', 1, 'hj', 'activity', '打印入口不开放'],
    ['1', 3, 'a2', 'activity', '只呈现报表正文\n隐去导航与按钮'],
    ['-1', 5, 'n1', 'note', '输出范围是当前的页面内容，不是全量数据'],
  ], [
    ['s', 'd1'], ['d1', 'hj', '否'], ['d1', 'a1', '是'], ['hj', 'e'], ['a1', 'a2'],
    ['a2', 'a4'], ['a4', 'e'],
  ]),
];

// ============ drawio 输出 ============
const escA = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/\n/g, '&#xa;');
const ACT_STYLE = {
  start: 'ellipse;html=1;fillColor=#000000;strokeColor=#000000;',
  end: 'ellipse;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=2;',
  activity: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#4d4d4d;',
  decision: 'rhombus;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;',
  db: 'shape=cylinder3;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;',
  note: 'shape=note;whiteSpace=wrap;html=1;fillColor=#ffe6cc;strokeColor=#d79b00;align=left;fontSize=11;',
};
const SIZE = {
  start: [26, 26], end: [34, 34], activity: [200, 56], decision: [210, 80], db: [170, 60], note: [300, 46],
};

// ============ 活动图纵向布局：分区为左右并排的竖列，流程自上而下 ============
// 纵向位置不取规约里的 col，而由 flow 的拓扑最长路径层级决定：
// 规约有 17 条边两端 col 相同，那是"先后"而非"并发"，照搬 col 会在竖版里读错语义。
const LANE_W = 280, LANE_STEP = 300, ROWSTEP = 130, LANE_HEAD = 34, TOP = 60, MAXH = 80;
const NOTE_W = 300;

function layoutAct(d) {
  const rows = d.rows.map(([lane, col, id, shape, label]) => ({
    id, shape, label, col, note: lane === '-1', lane: lane === '-1' ? null : Number(lane),
  }));
  const get = (id) => rows.find((r) => r.id === id);
  // 传递边 = 事件流的边 + 同一分区内按声明顺序的先后边。
  // 后者必须一起算：两栏下互斥分支常落在同一分区，若只按 flow 分层再事后挪层，
  // 挪完可能出现后继比前驱更靠上，「每条边都向下」就破了。
  const preds = {}, succs = {};
  const link = (u, v) => { (succs[u] ||= []).push(v); (preds[v] ||= []).push(u); };
  for (const [f, t] of d.flow) link(f, t);
  for (let i = 0; i < d.lanes.length; i++) {
    const inLane = rows.filter((r) => r.lane === i);
    for (let k = 1; k < inLane.length; k++) link(inLane[k - 1].id, inLane[k].id);
  }
  const indeg = {};
  rows.forEach((r) => { indeg[r.id] = (preds[r.id] || []).length; });
  const queue = rows.filter((r) => !indeg[r.id]).map((r) => r.id);
  const order = [];
  while (queue.length) {
    const u = queue.shift();
    order.push(u);
    for (const v of succs[u] || []) if (!--indeg[v]) queue.push(v);
  }
  if (order.length !== rows.length) throw new Error(d.file + '：事件流与分区内顺序互相矛盾（存在环），无法自上而下分层');
  const rank = {};
  for (const u of order) rank[u] = Math.max(0, ...(preds[u] || []).map((p) => rank[p] + 1));
  // note 不进传递图，按其声明的 col 对齐到同一步序节点的层级
  for (const r of rows) {
    if (!r.note) continue;
    const same = rows.filter((o) => !o.note && o.col === r.col);
    const earlier = rows.filter((o) => !o.note && o.col < r.col);
    rank[r.id] = same.length ? Math.max(...same.map((o) => rank[o.id]))
      : earlier.length ? Math.max(...earlier.map((o) => rank[o.id])) : 0;
  }
  const endNodes = rows.filter((r) => r.shape === 'end');
  const maxOther = Math.max(...rows.filter((r) => r.shape !== 'end').map((r) => rank[r.id]));
  endNodes.forEach((r) => { if (rank[r.id] <= maxOther) rank[r.id] = maxOther + 1; });

  // 备注列位置与画布宽度都按分区数推导，不写死栏数
  const lanesEnd = (d.lanes.length - 1) * LANE_STEP + LANE_W;
  const NOTE_X = lanesEnd + 20;
  const nodes = rows.map((r) => {
    const h = r.note ? SIZE.note[1] : SIZE[r.shape][1];
    const w = r.note ? NOTE_W : SIZE[r.shape][0];
    const x = r.note ? NOTE_X : r.lane * LANE_STEP + Math.round((LANE_W - w) / 2);
    return { ...r, rank: rank[r.id], x, y: TOP + rank[r.id] * ROWSTEP, w, h, cx: x + w / 2 };
  });
  const byAbs = Object.fromEntries(nodes.map((n) => [n.id, n]));
  const maxRank = Math.max(...nodes.map((n) => n.rank));
  const lanes = d.lanes.map((name, i) => ({ id: 'lane' + i, name, x: i * LANE_STEP, y: 0, w: LANE_W, h: TOP + maxRank * ROWSTEP + 100 }));
  const gutters = {};
  const channel = (s, t) => {
    if (s.lane === t.lane) {
      if (t.rank === s.rank + 1) return s.cx;
      return s.lane * LANE_STEP + LANE_W - 10;
    }
    const g = t.lane > s.lane ? s.lane : t.lane;
    const k = (gutters[g] = (gutters[g] || 0) + 1) - 1;
    return g * LANE_STEP + LANE_W + 6 + (k % 3) * 6;
  };
  const edges = d.flow.map(([f, t, lbl]) => {
    const s = byAbs[f], g = byAbs[t];
    const exitY = TOP + s.rank * ROWSTEP + MAXH + 12;
    const entryY = g.y - 20;
    const chX = channel(s, g);
    let pts = [[s.cx, s.y + s.h], [s.cx, exitY], [chX, exitY], [chX, entryY], [g.cx, entryY], [g.cx, g.y]];
    pts = pts.filter((p, i) => i === 0 || p[0] !== pts[i - 1][0] || p[1] !== pts[i - 1][1]);
    const lx = chX === g.cx ? g.cx + 8 : (chX + g.cx) / 2;
    const ly = chX === g.cx ? (exitY + g.y) / 2 : entryY - 6;
    return { from: f, to: t, label: lbl || '', pts, lx, ly, anchor: chX === g.cx ? 'start' : 'middle' };
  });
  const W = nodes.some((n) => n.note) ? NOTE_X + NOTE_W + 20 : lanesEnd + 20;
  return { nodes, lanes, edges, W, H: lanes[0].h };
}

function modelXml(d) {
  const cells = [];
  if (d.kind === 'usecase') {
    const b = d.boundary;
    cells.push(`<mxCell id="bnd" value="${escA(b.label)}" style="swimlane;html=1;startSize=30;fillColor=none;verticalAlign=top;fontStyle=1;horizontal=1;" vertex="1" parent="1"><mxGeometry x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" as="geometry"/></mxCell>`);
    for (const n of d.nodes) {
      const inside = n.col >= 3;
      const actor = n.shape === 'actor';
      const gx = inside ? x(n.col) - b.x : x(n.col);
      const gy = inside ? y(n.row) - b.y : y(n.row);
      const style = actor
        ? 'shape=umlActor;html=1;verticalLabelPosition=bottom;verticalAlign=top;outlineConnect=0;'
        : 'ellipse;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;';
      const [w, h] = actor ? [40, 80] : [190, 70];
      cells.push(`<mxCell id="${n.id}" value="${escA(n.label)}" style="${style}" vertex="1" parent="${inside ? 'bnd' : '1'}"><mxGeometry x="${gx}" y="${gy}" width="${w}" height="${h}" as="geometry"/></mxCell>`);
    }
    d.edges.forEach((e, i) => {
      const base = 'edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;strokeColor=#4d4d4d;';
      let style = base + 'endArrow=none;', value = '';
      if (e.style === 'include' || e.style === 'extend') { style = base + 'dashed=1;endArrow=open;'; value = e.style === 'include' ? '«include»' : '«extend»'; }
      else if (e.style === 'general') { style = base + 'endArrow=block;endFill=0;'; }
      cells.push(`<mxCell id="e${i}" value="${escA(value)}" style="${style}" edge="1" parent="1" source="${e.from}" target="${e.to}"><mxGeometry relative="1" as="geometry"/></mxCell>`);
    });
  } else {
    const L = layoutAct(d);
    for (const [li, ln] of L.lanes.entries()) {
      cells.push(`<mxCell id="${ln.id}" value="${escA(ln.name)}" style="swimlane;horizontal=1;startSize=${LANE_HEAD};fillColor=${LANE_COLOR[li]};html=1;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="${ln.x}" y="${ln.y}" width="${ln.w}" height="${ln.h}" as="geometry"/></mxCell>`);
    }
    for (const n of L.nodes) {
      const parent = n.note ? '1' : 'lane' + n.lane;
      const gx = n.note ? n.x : n.x - n.lane * LANE_STEP;
      cells.push(`<mxCell id="${n.id}" value="${escA(n.label)}" style="${ACT_STYLE[n.shape]}" vertex="1" parent="${parent}"><mxGeometry x="${gx}" y="${n.y}" width="${n.w}" height="${n.h}" as="geometry"/></mxCell>`);
    }
    L.edges.forEach((e, i) => {
      const style = 'edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;endArrow=block;strokeColor=#4d4d4d;exitX=0.5;exitY=1;entryX=0.5;entryY=0;';
      const mid = e.pts.slice(1, -1).map(([px, py]) => `<mxPoint x="${px}" y="${py}" />`).join('');
      const geo = mid ? `<mxGeometry relative="1" as="geometry"><Array as="points">${mid}</Array></mxGeometry>` : `<mxGeometry relative="1" as="geometry"/>`;
      cells.push(`<mxCell id="f${i}" value="${escA(e.label)}" style="${style}" edge="1" parent="1" source="${e.from}" target="${e.to}">${geo}</mxCell>`);
    });
  }
  return `<mxGraphModel dx="1200" dy="800" grid="1" gridSize="10" adaptiveColors="auto"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${cells.join('')}</root></mxGraphModel>`;
}
const toDrawio = (d) =>
  `<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net"><diagram id="${escA(d.file)}" name="${escA(d.title)}">${modelXml(d)}</diagram></mxfile>\n`;

// ============ SVG 输出 ============
function toSvg(d) {
  const L = d.kind === 'activity' ? layoutAct(d) : null;
  const W = d.kind === 'usecase' ? 1320 : L.W;
  const H = d.kind === 'usecase' ? 1400 : L.H;
  const body = [], pos = {};
  const txt = (cx, cy, s, size, anchor = 'middle') => {
    const lines = String(s).split('\n');
    lines.forEach((p, i) => body.push(`<text x="${Math.round(cx)}" y="${Math.round(cy + i * (size + 3) - (lines.length - 1) * (size / 2) + 4)}" font-family="Microsoft YaHei,PingFang SC,sans-serif" font-size="${size}" text-anchor="${anchor}" fill="#1c2b36">${p.replace(/[&<>]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]))}</text>`));
  };
  const seg = (a, b, dash, head) => body.push(`<line x1="${Math.round(a[0])}" y1="${Math.round(a[1])}" x2="${Math.round(b[0])}" y2="${Math.round(b[1])}" stroke="#4d4d4d" stroke-width="1.6"${dash ? ' stroke-dasharray="6 4"' : ''} marker-end="${head ? 'url(#ar)' : 'none'}"/>`);
  const center = (n) => pos[n];
  // 与 .drawio 共用 layoutAct 算出的折线点，两种输出不会再分叉
  const route = (pts) => body.push(`<polyline points="${pts.map(([px, py]) => `${Math.round(px)},${Math.round(py)}`).join(' ')}" fill="none" stroke="#4d4d4d" stroke-width="1.6" marker-end="url(#ar)"/>`);

  if (d.kind === 'activity') {
    for (const ln of L.lanes) body.push(`<rect x="${ln.x}" y="${ln.y}" width="${ln.w}" height="${ln.h}" fill="${LANE_COLOR[L.lanes.indexOf(ln)]}" stroke="#8a94a0"/>`);
    for (const ln of L.lanes) txt(ln.x + ln.w / 2, ln.y + LANE_HEAD / 2, ln.name, 13);
    for (const n of L.nodes) {
      const [left, top, w, h] = [n.x, n.y, n.w, n.h];
      const cx = n.cx, cy = top + h / 2;
      pos[n.id] = [cx, cy];
      if (n.shape === 'start') body.push(`<circle cx="${cx}" cy="${cy}" r="11" fill="#000"/>`);
      else if (n.shape === 'end') { body.push(`<circle cx="${cx}" cy="${cy}" r="14" fill="#fff" stroke="#000" stroke-width="2"/><circle cx="${cx}" cy="${cy}" r="7" fill="#000"/>`); }
      else if (n.shape === 'decision') { body.push(`<polygon points="${left},${cy} ${cx},${top} ${left + w},${cy} ${cx},${top + h}" fill="#fff2cc" stroke="#d6b656"/>`); txt(cx, cy, n.label, 11); }
      else if (n.shape === 'db') { body.push(`<rect x="${left}" y="${top + 8}" width="${w}" height="${h - 8}" fill="#d5e8d4" stroke="#82b366"/><ellipse cx="${cx}" cy="${top + 8}" rx="${w / 2}" ry="8" fill="#d5e8d4" stroke="#82b366"/>`); txt(cx, cy + 6, n.label, 11); }
      else if (n.shape === 'note') { body.push(`<rect x="${left}" y="${top}" width="${w}" height="${h}" fill="#ffe6cc" stroke="#d79b00"/>`); txt(left + 10, cy, n.label, 11, 'start'); }
      else { body.push(`<rect x="${left}" y="${top}" width="${w}" height="${h}" rx="10" fill="#fff" stroke="#4d4d4d"/>`); txt(cx, cy, n.label, 11); }
    }
    for (const e of L.edges) { route(e.pts); if (e.label) txt(e.lx, e.ly, e.label, 10, e.anchor); }
  } else {
    const b = d.boundary;
    body.push(`<rect x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" fill="none" stroke="#1c2b36"/>`);
    txt(b.x + 10, b.y + 18, b.label, 14, 'start');
    for (const n of d.nodes) {
      const actor = n.shape === 'actor';
      const gx = actor ? x(n.col) : x(n.col), gy = actor ? y(n.row) : y(n.row);
      if (actor) {
        pos[n.id] = [gx + 20, gy + 44];
        body.push(`<circle cx="${gx + 20}" cy="${gy + 12}" r="10" fill="none" stroke="#1c2b36" stroke-width="1.6"/><line x1="${gx + 20}" y1="${gy + 22}" x2="${gx + 20}" y2="${gy + 52}" stroke="#1c2b36" stroke-width="1.6"/><line x1="${gx}" y1="${gy + 34}" x2="${gx + 40}" y2="${gy + 34}" stroke="#1c2b36" stroke-width="1.6"/><line x1="${gx + 20}" y1="${gy + 52}" x2="${gx + 4}" y2="${gy + 76}" stroke="#1c2b36" stroke-width="1.6"/><line x1="${gx + 20}" y1="${gy + 52}" x2="${gx + 36}" y2="${gy + 76}" stroke="#1c2b36" stroke-width="1.6"/>`);
        txt(gx + 20, gy + 92, n.label, 12);
      } else {
        pos[n.id] = [gx + 95, gy + 35];
        body.push(`<ellipse cx="${gx + 95}" cy="${gy + 35}" rx="95" ry="35" fill="#dae8fc" stroke="#6c8ebf"/>`);
        txt(gx + 95, gy + 35, n.label, 11);
      }
    }
    d.edges.forEach((e) => {
      const a = center(e.from), b2 = center(e.to);
      const dash = e.style === 'include' || e.style === 'extend';
      seg(a, b2, dash, !!(e.style));
      if (e.style === 'general') body.push(`<polygon points="${b2[0]},${b2[1]} ${b2[0] - 14},${b2[1] - 7} ${b2[0] - 14},${b2[1] + 7}" fill="#fff" stroke="#4d4d4d"/>`);
      if (e.style) txt((a[0] + b2[0]) / 2, (a[1] + b2[1]) / 2 - 8, e.style === 'include' ? '«include»' : e.style === 'extend' ? '«extend»' : '', 10);
    });
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}"><defs><marker id="ar" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto"><path d="M0,0 L9,3 L0,6 z" fill="#4d4d4d"/></marker></defs><rect width="${W}" height="${H}" fill="#ffffff"/>${body.join('')}</svg>\n`;
}

const all = [USECASE, ...ACTS];
const index = [];
for (const d of all) {
  writeFileSync(`${OUT}/${d.file}.drawio`, toDrawio(d));
  writeFileSync(`${OUT}/${d.file}.svg`, toSvg(d));
  index.push({ file: d.file, title: d.title, kind: d.kind });
}
// 合并为单个多页 .drawio，便于一次性在 draw.io 里翻看全部图
const pages = all.map(d => `<diagram id="${escA(d.file)}" name="${escA(d.title)}">${modelXml(d)}</diagram>`);
writeFileSync(`${OUT}/模块四-全部图表.drawio`, `<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net">${pages.join('')}</mxfile>\n`);
console.log('生成 ' + all.length + ' 组：');
index.forEach(i => console.log('  ' + i.file + '  [' + i.kind + ']'));
