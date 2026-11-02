// fold-groups — 一回合内连续的工具卡片分三级视图：
//   L0  1 行摘要（分类统计）
//   L1  标题 + 每张卡 1 行（工具名 / 参数摘要 / 耗时）
//   L2  单卡原生全文
// 点标题行 L1→L0，点组头 L0→L1，点某一行翻该卡的全文。ctrl+o 只到 L1。
// edit/write 单独成组，不跟 ctrl+o 走。
// thinking 完全不管：用 pi 原生的 hideThinkingBlock 就够。
//
// 为什么分组必须惰性计算：pi 的 agent 循环里每个工具调用都是独立一条 assistant 消息，
// chatContainer 的子节点顺序是  Asst, Card, Asst, Card, …。所以不能按 addChild 顺序
// 增量维护分组，只能在 render 时按容器现状切段：纯 thinking 的 Asst 是「透明填充」，
// 不切断分组也不被本扩展管理；带正文的 Asst 断开分组。
//
// 删掉本文件 + /reload 即完全回原生。
// 调试：/fold debug 开关，决策写 %TEMP%/pi-fold-debug.log

import { createRequire } from "node:module"
import { existsSync, appendFileSync, writeFileSync, readFileSync } from "node:fs"
import { join } from "node:path"
import { tmpdir, homedir } from "node:os"
import type { ExtensionAPI } from "@earendil-works/pi-coding-agent"

const EDIT_TOOLS = new Set(["edit", "write"])
let debugEnabled = process.env.PI_FOLD_DEBUG === "1"
const LOG_PATH = join(tmpdir(), "pi-fold-debug.log")
const STATUS_PATH = join(tmpdir(), "pi-fold-status.log")
const CONFIG_PATH = join(homedir(), ".pi", "agent", "fold-groups.json")

/** previewLines: how many lines an expanded card shows. 0 = no limit. */
type Cfg = { previewLines: number }
const DEFAULT_CFG: Cfg = { previewLines: 40 }

function loadCfg(): Cfg {
  try {
    const raw = JSON.parse(readFileSync(CONFIG_PATH, "utf8")) as Partial<Cfg>
    const n = Number(raw.previewLines)
    return { previewLines: Number.isFinite(n) && n >= 0 ? Math.floor(n) : DEFAULT_CFG.previewLines }
  } catch {
    return { ...DEFAULT_CFG }
  }
}

let CFG: Cfg = loadCfg()

function saveCfg(next: Cfg): void {
  CFG = next
  try {
    writeFileSync(CONFIG_PATH, `${JSON.stringify(next, null, 2)}\n`)
  } catch {
    /* unwritable config must not break the session */
  }
}

function appendLog(msg: string): void {
  try {
    appendFileSync(LOG_PATH, `${new Date().toISOString()} ${msg}\n`)
  } catch {
    /* logging must never break the session */
  }
}

function dbg(msg: string): void {
  if (!debugEnabled) return
  appendLog(msg)
}

/** Errors and click decisions are rare and load-bearing — always record them. */
function dbgAlways(msg: string): void {
  appendLog(msg)
}

/** One line per launch, always written — tells us whether the extension ran at all. */
function status(msg: string): void {
  try {
    writeFileSync(
      STATUS_PATH,
      `${new Date().toISOString()} pid=${process.pid}\nargv=${process.argv.slice(0, 3).join(" ")}\n${msg}\n`,
    )
  } catch {
    /* ignore */
  }
}

function findPiEntry(): string[] {
  // pi ships TWO builds of the same classes:
  //   dist/index.js         – unbundled barrel; a DIFFERENT class object than the one
  //                           the running process instantiates
  //   dist/bundle/index.js  – the bundle the runtime actually imports
  // Patching the barrel patches nothing that is ever constructed, so the bundle entry
  // must come first. Verified: require(dist/bundle/index.js).ToolExecutionComponent
  // === the class inside dist/bundle/chunks/chunk-*.js at runtime.
  const out: string[] = []
  const add = (p: string) => {
    if (p && existsSync(p) && !out.includes(p)) out.push(p)
  }
  for (const a of process.argv.slice(0, 3)) {
    if (!a) continue
    const m = a.match(/^(.*[\\/]pi-coding-agent)[\\/]dist[\\/]/)
    if (m) {
      add(join(m[1], "dist", "bundle", "index.js"))
      add(join(m[1], "dist", "index.js"))
    }
  }
  const roots: string[] = []
  if (process.env.APPDATA) roots.push(join(process.env.APPDATA, "npm", "node_modules"))
  if (process.env.npm_config_prefix) roots.push(join(process.env.npm_config_prefix, "node_modules"))
  roots.push(join(process.cwd(), "node_modules"))
  for (const root of roots) {
    add(join(root, "@earendil-works", "pi-coding-agent", "dist", "bundle", "index.js"))
    add(join(root, "@earendil-works", "pi-coding-agent", "dist", "index.js"))
  }
  return out
}

// ---------- types ----------

type MouseEvt = { type: string; button?: string; x: number; y: number; width: number; height: number }

type Card = {
  toolName: string
  toolCallId: string
  expanded: boolean
  args?: Record<string, unknown>
  result?: { durationMs?: number; isError?: boolean }
  ui?: unknown
  render(width: number): string[]
  handleMouse(event: MouseEvt): unknown
  setExpanded(expanded: boolean): void
}

type ContainerLike = { children: unknown[] }

type Group = {
  start: number
  end: number
  head: Card
  cards: Card[]
  kind: "tool" | "edit"
  nameBy?: Map<Card, string>
}

type Segments = { mut: number; groups: Group[]; byChild: Map<object, Group> }

// State lives on globalThis so that a /reload (which re-evaluates this file because pi's
// loader uses moduleCache:false) reuses the bookkeeping instead of orphaning every card
// created before the reload.
type Global = {
  mut: number
  parentOf: WeakMap<object, ContainerLike>
  /** group level: 0 = L0 summary, 1 = L1 list. Per-card full text uses card.expanded. */
  openState: WeakMap<object, number>
  segCache: WeakMap<ContainerLike, Segments>
  knownContainers: Set<ContainerLike>
  TEC: (new (...a: unknown[]) => unknown) | undefined
  AMC: (new (...a: unknown[]) => unknown) | undefined
  orig: Record<string, unknown>
}

const G: Global = ((globalThis as Record<string, unknown>).__foldGroups ??= {
  mut: 0,
  parentOf: new WeakMap(),
  openState: new WeakMap(),
  segCache: new WeakMap(),
  knownContainers: new Set(),
  TEC: undefined,
  AMC: undefined,
  orig: {},
} as Global)

// ---------- text helpers (no pi-tui import: keep the extension dependency-free) ----------

function charWidth(cp: number): number {
  if (cp < 32) return 0
  if (
    (cp >= 0x1100 && cp <= 0x115f) ||
    (cp >= 0x2e80 && cp <= 0xa4cf) ||
    (cp >= 0xac00 && cp <= 0xd7a3) ||
    (cp >= 0xf900 && cp <= 0xfaff) ||
    (cp >= 0xfe30 && cp <= 0xfe6f) ||
    (cp >= 0xff00 && cp <= 0xff60) ||
    (cp >= 0xffe0 && cp <= 0xffe6) ||
    (cp >= 0x1f300 && cp <= 0x1f64f) ||
    (cp >= 0x1f900 && cp <= 0x1f9ff) ||
    (cp >= 0x20000 && cp <= 0x3fffd)
  )
    return 2
  return 1
}

function visibleWidth(s: string): number {
  let w = 0
  for (const ch of s) w += charWidth(ch.codePointAt(0) ?? 0)
  return w
}

function truncateToWidth(s: string, width: number): string {
  if (width <= 0) return ""
  if (visibleWidth(s) <= width) return s
  let out = ""
  let w = 0
  for (const ch of s) {
    const cw = charWidth(ch.codePointAt(0) ?? 0)
    if (w + cw > width - 1) break
    out += ch
    w += cw
  }
  return out + "…"
}

// ---------- classification ----------

function isCard(c: unknown): c is Card {
  return !!c && !!G.TEC && c instanceof (G.TEC as { new (...a: unknown[]): unknown })
}

function ctorName(c: unknown): string {
  return ((c as { constructor?: { name?: string } })?.constructor?.name ?? "")
}

function isAssistant(c: unknown): boolean {
  if (!c) return false
  if (G.AMC) return c instanceof (G.AMC as { new (...a: unknown[]): unknown })
  return ctorName(c) === "AssistantMessageComponent"
}

/**
 * Does this assistant component carry real prose? Anything we cannot prove is
 * prose-free is treated as prose, so the run breaks and we over-show rather than
 * swallow text.
 */
function hasRealText(c: unknown): boolean {
  const msg = (c as { lastMessage?: { content?: unknown } })?.lastMessage
  if (!msg || !Array.isArray(msg.content)) return true
  for (const part of msg.content as Array<{ type?: string; text?: string }>) {
    if (part?.type === "text" && typeof part.text === "string" && part.text.trim() !== "") return true
  }
  return false
}

function isFiller(c: unknown): boolean {
  return isAssistant(c) && !hasRealText(c)
}

function cardKind(c: Card): "tool" | "edit" {
  return EDIT_TOOLS.has(c.toolName ?? "") ? "edit" : "tool"
}

// ---------- grouping (lazy, per container state) ----------

function segments(container: ContainerLike): Segments {
  const cached = G.segCache.get(container)
  if (cached && cached.mut === G.mut) return cached

  const kids = container.children
  const groups: Group[] = []
  const byChild = new Map<object, Group>()
  let i = 0
  while (i < kids.length) {
    const first = kids[i]
    if (!isCard(first)) {
      i++
      continue
    }
    const kind = cardKind(first)
    const cards: Card[] = [first]
    let end = i
    let j = i + 1
    while (j < kids.length) {
      const nxt = kids[j]
      if (isCard(nxt)) {
        if (cardKind(nxt) !== kind) break
        cards.push(nxt)
        end = j
        j++
        continue
      }
      if (isFiller(nxt)) {
        // keep the run open only if a same-kind card follows the filler run
        let k = j
        while (k < kids.length && isFiller(kids[k])) k++
        if (k < kids.length && isCard(kids[k]) && cardKind(kids[k]) === kind) {
          j = k
          continue
        }
        break
      }
      break
    }
    // never hide the tail: the group ends on a card
    while (end > i && !isCard(kids[end])) end--
    const g: Group = { start: i, end, head: cards[0], cards, kind }
    groups.push(g)
    // Only cards are mapped: fillers ("Thinking...") are rendered natively and must
    // never be told they belong to a group.
    for (const c of cards) byChild.set(c as unknown as object, g)
    i = end + 1
  }

  const rec: Segments = { mut: G.mut, groups, byChild }
  G.segCache.set(container, rec)
  dbg(`segments container=${kids.length}kids groups=${groups.map((x) => `${x.kind}:${x.cards.length}`).join(",")}`)
  return rec
}

function groupOf(child: unknown): Group | undefined {
  if (!child || typeof child !== "object") return undefined
  const container = G.parentOf.get(child as object)
  if (!container) return undefined
  return segments(container).byChild.get(child as object)
}

function levelOf(g: Group): number {
  return G.openState.get(g.head as unknown as object) ?? 0
}

function fmtDur(ms: number): string {
  return ms < 1000 ? `${Math.round(ms)}ms` : `${(ms / 1000).toFixed(ms < 10000 ? 1 : 0)}s`
}

/**
 * `mcp__server__tool` renders as `tool`, unless two cards in the same group share that
 * tail — then those cards fall back to `server/tool` so they stay distinguishable.
 */
function displayToolName(card: Card, g: Group): string {
  if (!g.nameBy) {
    const shortOf = (name: string) => {
      const parts = name.split("__")
      return parts[0] === "mcp" && parts.length >= 3 ? parts[parts.length - 1] : name
    }
    const counts = new Map<string, number>()
    for (const c of g.cards) {
      const s = shortOf(c.toolName ?? "")
      counts.set(s, (counts.get(s) ?? 0) + 1)
    }
    g.nameBy = new Map()
    for (const c of g.cards) {
      const name = c.toolName ?? ""
      const parts = name.split("__")
      const s = shortOf(name)
      const dup = (counts.get(s) ?? 0) > 1
      g.nameBy.set(c, dup && parts[0] === "mcp" && parts.length >= 3 ? `${parts[1]}/${parts[parts.length - 1]}` : s)
    }
  }
  return g.nameBy.get(card) ?? card.toolName ?? ""
}

const ARG_KEYS = ["command", "path", "file_path", "pattern", "query", "url", "task", "prompt"]

function argSummary(card: Card): string {
  const a = card.args
  if (!a || typeof a !== "object") return ""
  for (const key of ARG_KEYS) {
    const v = (a as Record<string, unknown>)[key]
    if (typeof v === "string" && v.trim() !== "") return v.replace(/\s+/g, " ").trim()
  }
  for (const v of Object.values(a as Record<string, unknown>)) {
    if (typeof v === "string" && v.trim() !== "") return v.replace(/\s+/g, " ").trim()
  }
  return ""
}

/** L1 row: two-space indent, tool name, argument summary, duration right-aligned. */
function toolLine(card: Card, width: number): string {
  const name = displayToolName(card, groupOfCard(card)!)
  const arg = argSummary(card)
  const d = card.result?.durationMs
  const dur = typeof d === "number" && Number.isFinite(d) ? fmtDur(d) : ""
  const head = `  ${name}${arg ? `  ${arg}` : ""}`
  if (dur) {
    const room = width - visibleWidth(dur) - 1
    if (visibleWidth(head) < room) return truncateToWidth(head + " ".repeat(room - visibleWidth(head)) + " " + dur, width)
    return truncateToWidth(`${head} ${dur}`, width)
  }
  return truncateToWidth(head, width)
}

function groupOfCard(card: Card): Group | undefined {
  return groupOf(card)
}

/** L0/L1 title line with per-tool-kind counts, e.g. `● 9 个工具调用 · bash 5 · read 4 (2s) ⚠1 ▸`. */
function summaryLine(g: Group, open: boolean): string {
  const n = g.cards.length
  const isEdit = g.kind === "edit"
  let ms = 0
  let timed = false
  let errs = 0
  for (const c of g.cards) {
    const d = c.result?.durationMs
    if (typeof d === "number" && Number.isFinite(d)) {
      ms += d
      timed = true
    }
    if (c.result?.isError) errs++
  }
  const counts = new Map<string, number>()
  for (const c of g.cards) {
    const name = displayToolName(c, g)
    counts.set(name, (counts.get(name) ?? 0) + 1)
  }
  const ranked = [...counts.entries()].sort((a, b) => b[1] - a[1])
  const top = ranked.slice(0, 3).map(([name, cnt]) => ` · ${name} ${cnt}`).join("")
  const rest = ranked.length > 3 ? ` · +${ranked.length - 3}` : ""
  const dur = timed && ms > 0 ? ` (${fmtDur(ms)})` : ""
  const bad = errs > 0 ? ` ⚠${errs}` : ""
  const icon = isEdit ? "✎" : "●"
  const label = isEdit ? `${n} 个文件改动` : `${n} 个工具调用`
  return `${icon} ${label}${top}${rest}${dur}${bad} ${open ? "▾" : "▸"}`
}

function requestRender(g: Group): void {
  for (const c of g.cards) {
    try {
      const ui = c.ui as { requestRender?: () => void } | undefined
      if (ui && typeof ui.requestRender === "function") {
        ui.requestRender()
        return
      }
    } catch {
      /* ignore */
    }
  }
}

/** Change the group level only — per-card full text lives in card.expanded and is untouched. */
function setGroupLevel(g: Group, level: number, source: string): void {
  if (level === 0) centerCard(g.head)
  G.openState.set(g.head as unknown as object, level)
  requestRender(g)
  dbgAlways(`level ${g.kind} n=${g.cards.length} -> ${level} src=${source} rendered=${g.head.renderCount ?? "?"}`)
}

/** A one-card group has no list level: the summary line and the full body are the only two
 * states, so `card.expanded` alone drives it. Going through L1 here used to render a second,
 * near-identical row that swallowed the click. */
function setSingleCardGroup(g: Group, open: boolean, source: string): void {
  dbgAlways(`single n=1 ${g.kind} ${open ? "open" : "close"} src=${source}`)
  setCardExpanded(g.head, open)
  requestRender(g)
}

function setCardExpanded(card: Card, value: boolean): void {
  // Do not go through any captured setExpanded: a stale closure from an earlier /reload can
  // make the call a no-op, which looked exactly like "clicking does nothing". The native
  // method is a one-liner (expanded = v; updateDisplay()), and updateDisplay is what rebuilds
  // the renderer children that actually hold the content, so both steps are done here.
  card.expanded = value
  const ud = (card as unknown as { updateDisplay?: () => void }).updateDisplay
  if (typeof ud === "function") ud.call(card)
  dbgAlways(`setExpanded ${card.toolName} want=${value} -> ${card.expanded} hasUD=${typeof ud === "function"}`)
}

function toggleCard(card: Card, g: Group): void {
  // centering must use the layout from before the collapse: folding only removes lines
  // below this card, so its own document row is unchanged.
  if (card.expanded) centerCard(card)
  setCardExpanded(card, !card.expanded)
  requestRender(g)
}

// ---------- expanded-body shaping ----------

const ansiRe = /\u001b\[[0-9;]*m|\u001b\][0-9];[^\u0007\u001b]*\u0007/g
const isBlank = (l: string) => l.replace(ansiRe, "").trim() === ""

/** Keep the first `max` lines and say how many are hidden. `max <= 0` means unlimited. */
function capLines(lines: string[], max: number): string[] {
  if (max <= 0 || lines.length <= max) return lines
  return [...lines.slice(0, max), `… 还有 ${lines.length - max} 行（再点一次收起）`]
}

/**
 * pi renders an edit diff only from `preview.diff` or `result.details.diff`. Cards without
 * either (replayed sessions, older results) render nothing but the path, so an expanded
 * card would look identical to a collapsed one. Rebuild a readable diff from the args.
 */
function fallbackBody(card: Card): string[] {
  const a = card.args as Record<string, unknown> | undefined
  if (!a) return []
  const pushAll = (prefix: string, text: unknown, out: string[]) => {
    if (typeof text !== "string") return
    for (const l of text.split("\n")) out.push(`${prefix}${l}`)
  }
  const out: string[] = []
  if (card.toolName === "write") {
    pushAll("  ", a.content, out)
  } else if (card.toolName === "edit") {
    const edits = a.edits
    if (Array.isArray(edits)) {
      for (const e of edits as Array<{ oldText?: unknown; newText?: unknown }>) {
        pushAll("  - ", e?.oldText, out)
        pushAll("  + ", e?.newText, out)
      }
    } else {
      pushAll("  - ", a.oldText, out)
      pushAll("  + ", a.newText, out)
    }
  }
  return out
}

/** Native body for an expanded card, with the arg fallback and the preview cap applied. */
function expandedBody(card: Card, width: number, orig: (this: unknown, w: number) => string[]): string[] {
  let body = orig.call(card, width)
  if (EDIT_TOOLS.has(card.toolName ?? "") && body.filter((l) => !isBlank(l)).length <= 1) {
    const fb = fallbackBody(card)
    if (fb.length > 0) body = [...body.filter((l) => !isBlank(l)).slice(0, 1), ...fb]
  }
  return capLines(body, CFG.previewLines)
}

// ---------- scroll centering ----------

type LayoutEntry = { component: unknown; height: number }
type Layout = { children?: LayoutEntry[] }

function childRow(entries: LayoutEntry[] | undefined, target: unknown): number | undefined {
  if (!entries) return undefined
  let row = 0
  for (const e of entries) {
    if (e.component === target) return row
    row += e.height
  }
  return undefined
}

/**
 * Absolute document line of `card`, using the layout cached by the last frame — no
 * re-render. Collapsing only removes lines *below* this card, so the value stays valid.
 */
function docRowOf(card: Card): number | undefined {
  const chat = G.parentOf.get(card as unknown as object)
  if (!chat) return undefined
  let tui: Record<string, unknown>
  try {
    tui = card.ui as Record<string, unknown>
    if (!tui || typeof tui !== "object") return undefined
  } catch {
    return undefined
  }
  const above = childRow((tui.mouseLayout as Layout | undefined)?.children, chat)
  if (above === undefined) return undefined
  const inside = childRow((chat as { mouseLayout?: Layout }).mouseLayout?.children, card)
  return inside === undefined ? undefined : above + inside
}

/** Scroll so `card` sits in the middle of the viewport. Silently no-ops if pi moves it. */
function centerCard(card: Card): void {
  const row = docRowOf(card)
  if (row === undefined) return
  const sv = (() => {
    try {
      return (card.ui as { implicitScrollView?: { scrollTo?: (n: number) => void; viewportHeight?: number } })
        ?.implicitScrollView
    } catch {
      return undefined
    }
  })()
  if (!sv || typeof sv.scrollTo !== "function") return
  const vh = sv.viewportHeight ?? 0
  if (vh <= 0) return
  sv.scrollTo(Math.max(0, row - Math.floor(vh / 2)))
  dbg(`centerCard ${card.toolName} row=${row} vh=${vh}`)
}

// ---------- shared patches ----------

function foldRender(self: unknown, width: number, orig: (this: unknown, w: number) => string[]): string[] {
  const g = groupOf(self)
  if (!g) return orig.call(self, width)
  try {
    return renderGroup(self as Card, g, width, orig)
  } catch (err) {
    dbgAlways(`render failed, falling back to native: ${String(err)}`)
    return orig.call(self, width)
  }
}

function renderGroup(card: Card, g: Group, width: number, orig: (this: unknown, w: number) => string[]): string[] {
  // A one-card group only ever shows a summary line or its own body.
  if (g.cards.length === 1) {
    if (!card.expanded) return [truncateToWidth(summaryLine(g, false), width)]
    return [truncateToWidth(summaryLine(g, true), width), ...expandedBody(card, width, orig)]
  }
  const level = levelOf(g)
  const isHead = card === g.head

  if (level === 0) {
    // Full text never leaks through L0 (decision 7 keeps card.expanded across collapses).
    if (!isHead) return []
    const line = truncateToWidth(summaryLine(g, false), width)
    dbg(`L0 head kind=${g.kind} n=${g.cards.length}`)
    return [line]
  }

  // L1: the title row always sits on the head, even when the head itself is open, so
  // there is always something to click back to L0. Any row may be open at this level.
  const body = card.expanded ? expandedBody(card, width, orig) : [toolLine(card, width)]
  if (isHead) return [truncateToWidth(summaryLine(g, true), width), ...body]
  return body
}

function foldMouse(self: Card, event: MouseEvt, orig: (this: Card, e: MouseEvt) => unknown): unknown {
  const g = groupOf(self)
  if (!g) return orig.call(self, event) // ungrouped card: native behaviour untouched
  try {
    return mouseInGroup(self, g, event, orig)
  } catch (err) {
    dbgAlways(`mouse failed, falling back to native: ${String(err)}`)
    return orig.call(self, event)
  }
}

function mouseInGroup(self: Card, g: Group, event: MouseEvt, orig: (this: Card, e: MouseEvt) => unknown): unknown {
  if (event.type !== "click" || event.button !== "left") return orig.call(self, event)
  dbgAlways(`click kind=${g.kind} n=${g.cards.length} y=${event.y} head=${self === g.head} exp=${self.expanded} level=${levelOf(g)}`)

  const level = levelOf(g)
  const isHead = (self as unknown) === (g.head as unknown)

  if (g.cards.length === 1) {
    setSingleCardGroup(g, !g.head.expanded, "click")
    return { handled: true }
  }

  if (level === 0) {
    // Any card of a closed group opens the list. A cached mouseLayout can lag one frame
    // behind a fold, so a click may land on a non-head card; requiring the head there made
    // the group look dead to clicks.
    setGroupLevel(g, 1, "click")
    return { handled: true }
  }

  // L1: y === 0 on the head is the title row -> collapse to L0 (keep card.expanded).
  if (isHead && event.y === 0) {
    setGroupLevel(g, 0, "click")
    return { handled: true }
  }

  // Everything else in L1 toggles this card's full text. We never forward to the native
  // handler: its click callback only calls setExpanded(), which is now reserved for ctrl+o.
  toggleCard(self, g)
  return { handled: true }
}

/**
 * setExpanded entry point (ctrl+o, and pi itself when it expands a card on completion).
 * Always applies to the card so nothing can silently refuse to open, then syncs the group
 * level so a multi-card group becomes visible.
 */
function foldSetExpanded(self: Card, expanded: boolean, orig: (this: Card, v: boolean) => void): void {
  const g = groupOf(self)
  if (!g) {
    orig.call(self, expanded)
    return
  }
  if (g.cards.length === 1) {
    // No list level exists: open/close the body directly.
    setCardExpanded(self, expanded)
    requestRender(g)
    return
  }
  // Multi-card: ctrl+o toggles only the list level (decision 6), through the head. A card
  // keeps whatever per-card expansion the user clicked in; a bare setExpanded must not
  // silently open every body.
  if (self === g.head) setGroupLevel(g, expanded ? 1 : 0, "ctrl+o")
}

// ---------- patching ----------

function patch(pi: ExtensionAPI): boolean {
  const candidates = findPiEntry()
  if (candidates.length === 0) {
    dbg("pi entry not found — extension inactive (native behaviour kept)")
    return false
  }
  for (const entry of candidates) {
    if (tryPatch(entry)) {
      dbg(`patched via ${entry}`)
      return true
    }
  }
  dbg(`no patchable ToolExecutionComponent in ${candidates.join(" | ")}`)
  return false
}

function tryPatch(entry: string): boolean {
  let mod: Record<string, unknown>
  try {
    // pi's loader gives extensions a different module instance (jiti moduleCache:false),
    // so the class must come from the very entry the running process uses.
    mod = createRequire(entry)(entry) as Record<string, unknown>
  } catch (err) {
    dbg(`require(${entry}) failed: ${String(err)}`)
    return false
  }
  const TEC = mod.ToolExecutionComponent as { prototype: Record<string, unknown> } | undefined
  const tproto = TEC?.prototype
  if (!tproto || typeof tproto.render !== "function" || typeof tproto.setExpanded !== "function" || typeof tproto.handleMouse !== "function") {
    dbg(`ToolExecutionComponent surface unexpected in ${entry} — skipping`)
    return false
  }
  const AMC = mod.AssistantMessageComponent as { prototype: Record<string, unknown> } | undefined
  const ContainerProto = Object.getPrototypeOf(tproto) as Record<string, unknown> | undefined
  if (!ContainerProto || typeof ContainerProto.addChild !== "function") {
    dbg("Container prototype unexpected — extension inactive")
    return false
  }

  G.TEC = TEC as unknown as new (...a: unknown[]) => unknown
  G.AMC = AMC as unknown as new (...a: unknown[]) => unknown | undefined

  // Restore a previous install before re-wrapping: pi reloads this file on every
  // /reload, and stacking wrappers would compound — worse, the kept G.orig would point
  // at the PREVIOUS install's fold functions, so old logic keeps running inside bodies.
  const o = G.orig
  const t = tproto as Record<string, any>
  if (o.render) {
    t.render = o.render
    t.handleMouse = o.handleMouse
    t.setExpanded = o.setExpanded
    ContainerProto.addChild = o.addChild
    ContainerProto.removeChild = o.removeChild
    ContainerProto.clear = o.clear
  } else {
    o.render = t.render
    o.handleMouse = t.handleMouse
    o.setExpanded = t.setExpanded
    o.addChild = ContainerProto.addChild
    o.removeChild = ContainerProto.removeChild
    o.clear = ContainerProto.clear
  }

  t.render = function (this: unknown, width: number) {
    return foldRender(this, width, o.render as (this: unknown, w: number) => string[])
  }
  t.handleMouse = function (this: Card, event: MouseEvt) {
    return foldMouse(this, event, o.handleMouse as (this: Card, e: MouseEvt) => unknown)
  }
  t.setExpanded = function (this: Card, expanded: boolean) {
    foldSetExpanded(this, expanded, o.setExpanded as (this: Card, v: boolean) => void)
  }

  // Container.prototype is shared by every container (chatContainer, message components,
  // boxes). Any child add/remove can change a run's boundaries, so bump the generation
  // counter to invalidate cached segmentations.
  ContainerProto.addChild = function (this: ContainerLike, child: unknown) {
    const r = (o.addChild as (this: ContainerLike, c: unknown) => unknown).call(this, child)
    if (child && typeof child === "object") {
      G.parentOf.set(child as object, this)
      if (isCard(child)) G.knownContainers.add(this)
    }
    G.mut++
    return r
  }
  ContainerProto.removeChild = function (this: ContainerLike, child: unknown) {
    G.mut++
    return (o.removeChild as (this: ContainerLike, c: unknown) => unknown).call(this, child)
  }
  ContainerProto.clear = function (this: ContainerLike) {
    G.mut++
    return (o.clear as (this: ContainerLike) => unknown).call(this)
  }

  t.__foldGroupsPatched = true
  return true
}

// ---------- extension entry ----------

export default async function (pi: ExtensionAPI) {
  const ok = patch(pi)
  status(ok ? "ACTIVE" : "INACTIVE (native behaviour kept)")

  pi.registerCommand("fold", {
    description: "fold-groups: /fold 查看状态，/fold debug 调试日志，/fold lines [n] 设置展开后显示多少行",
    handler: async (args, ctx) => {
      const ui = (ctx as unknown as { ui?: { showStatus?: (s: string) => void; requestRender?: () => void } }).ui
      const say = (s: string) => {
        dbg(`cmd: ${s}`)
        ui?.showStatus?.(s)
        ui?.requestRender?.()
      }
      const arg = (args ?? "").trim().toLowerCase()
      if (arg === "debug") {
        debugEnabled = !debugEnabled
        say(`fold debug ${debugEnabled ? "on" : "off"} → ${LOG_PATH}`)
        return
      }
      if (arg === "lines" || arg.startsWith("lines ")) {
        const n = Number(arg.slice(5).trim())
        if (arg === "lines" || !Number.isFinite(n) || n < 0) {
          say(`fold previewLines=${CFG.previewLines}${CFG.previewLines === 0 ? " (不限)" : ""} → ${CONFIG_PATH}`)
          return
        }
        saveCfg({ ...CFG, previewLines: Math.floor(n) })
        say(`fold previewLines=${CFG.previewLines}${CFG.previewLines === 0 ? " (不限)" : ""} 已保存`)
        return
      }
      if (arg === "on" || arg === "off") {
        const level = arg === "on" ? 1 : 0
        let n = 0
        for (const c of G.knownContainers) {
          for (const g of segments(c).groups) {
            setGroupLevel(g, level, "command")
            n++
          }
        }
        say(`fold: ${n} 组${level ? "展开到列表" : "折叠"}`)
        return
      }
      say(`fold ${ok ? "ACTIVE" : "INACTIVE"} containers=${G.knownContainers.size} gen=${G.mut}`)
    },
  })
}
