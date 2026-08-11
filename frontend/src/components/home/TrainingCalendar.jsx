import { memo, useMemo, useState } from "react";
import * as Dialog from "@radix-ui/react-dialog";
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Dices,
  Flame,
  Sparkles,
  Star,
  Tag,
  X,
} from "lucide-react";

const WEEKDAYS = ["一", "二", "三", "四", "五", "六", "日"];

function TrainingCalendar({ data = [], onCheckIn, loadError = "" }) {
  const today = useMemo(() => startOfDay(new Date()), []);
  const todayKey = formatDate(today);
  const [visibleMonth, setVisibleMonth] = useState(() => new Date(today.getFullYear(), today.getMonth(), 1));
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedFortune, setSelectedFortune] = useState(null);
  const [drawing, setDrawing] = useState(false);
  const [drawError, setDrawError] = useState("");
  const itemMap = useMemo(() => new Map(data.map((item) => [item.date, item])), [data]);
  const summary = useMemo(() => buildSummary(itemMap, today, visibleMonth), [itemMap, today, visibleMonth]);
  const cells = useMemo(() => buildMonthCells(visibleMonth, itemMap, today), [visibleMonth, itemMap, today]);
  const todayItem = itemMap.get(todayKey);
  const earliestMonth = useMemo(() => {
    const firstDate = data.map((item) => item.date).sort()[0];
    const parsed = firstDate ? parseDate(firstDate) : new Date(today.getFullYear(), today.getMonth() - 11, 1);
    return new Date(parsed.getFullYear(), parsed.getMonth(), 1);
  }, [data, today]);
  const isCurrentMonth = sameMonth(visibleMonth, today);
  const canGoBack = visibleMonth > earliestMonth;

  function moveMonth(offset) {
    setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() + offset, 1));
  }

  function revealFortune(item) {
    if (!item) return;
    setSelectedFortune(item);
    setDrawError("");
    setDialogOpen(true);
  }

  async function drawTodayFortune() {
    if (todayItem) {
      revealFortune(todayItem);
      return;
    }
    if (!onCheckIn || drawing) return;
    setDrawing(true);
    setDrawError("");
    try {
      const item = await onCheckIn();
      revealFortune(item);
    } catch (error) {
      setDrawError(error?.message || "签到失败，请稍后重试");
    } finally {
      setDrawing(false);
    }
  }

  return (
    <>
      <section className="training-calendar-card render-lazy">
        <div className="training-calendar-main">
          <div className="training-calendar-head">
            <div>
              <span className="calendar-kicker"><CalendarDays size={16} />趣味签到日历</span>
              <h2>{visibleMonth.getFullYear()} 年 {visibleMonth.getMonth() + 1} 月</h2>
            </div>
            <div className="calendar-controls">
              <button type="button" onClick={() => moveMonth(-1)} disabled={!canGoBack} aria-label="上个月"><ChevronLeft size={18} /></button>
              {!isCurrentMonth && <button type="button" className="calendar-today-button" onClick={() => setVisibleMonth(new Date(today.getFullYear(), today.getMonth(), 1))}>本月</button>}
              <button type="button" onClick={() => moveMonth(1)} disabled={isCurrentMonth} aria-label="下个月"><ChevronRight size={18} /></button>
            </div>
          </div>

          <div className="training-calendar-grid" role="grid" aria-label={`${visibleMonth.getMonth() + 1} 月签到日历`}>
            {WEEKDAYS.map((day) => <span key={day} className="calendar-weekday">{day}</span>)}
            {cells.map((cell, index) => cell ? (
              <button
                type="button"
                key={cell.key}
                className={`calendar-day ${cell.item ? "is-active" : ""} ${cell.today ? "is-today" : ""} ${cell.future ? "is-future" : ""}`}
                title={cell.item ? `${cell.key} · ${cell.item.fortuneTitle}` : cell.key}
                aria-label={`${cell.key}${cell.item ? `，${cell.item.fortuneTitle}` : "，未签到"}`}
                onClick={() => revealFortune(cell.item)}
                disabled={!cell.item}
              >
                <span>{cell.day}</span>
                {cell.item && <i aria-hidden="true" style={{ color: cell.item.luckyColor || undefined }}><Sparkles size={12} /></i>}
              </button>
            ) : <span key={`empty-${index}`} className="calendar-day is-empty" aria-hidden="true" />)}
          </div>
        </div>

        <aside className="training-calendar-aside">
          <div className={`calendar-stamp ${todayItem ? "is-lit" : ""}`}>
            <span>{todayItem ? "今日已签到" : "等待抽签"}</span>
            <strong>{today.getDate()}</strong>
            <small>{today.getMonth() + 1} 月</small>
          </div>
          <div className="calendar-summary-copy">
            <span className="calendar-kicker"><Flame size={16} />签到连续记录</span>
            <h3>{todayItem ? todayItem.fortuneTitle : summary.streak > 0 ? `已经连续 ${summary.streak} 天` : "今天抽一枚训练签"}</h3>
            <p>{todayItem ? todayItem.fortuneMessage : "手动签到并抽取今日运势。它只记录你的到来，不影响每日一题完成状态。"}</p>
          </div>
          <div className="calendar-mini-stats">
            <div><span>本月签到</span><strong>{summary.monthActive} 天</strong></div>
            <div><span>最长连续</span><strong>{summary.longestStreak} 天</strong></div>
          </div>
          <div className="weekly-energy">
            <div><span>本周足迹</span><strong>{summary.weekActive}/7</strong></div>
            <div className="weekly-energy-track">
              {Array.from({ length: 7 }, (_, index) => <i key={index} className={index < summary.weekActive ? "is-filled" : ""} />)}
            </div>
          </div>
          {(loadError || drawError) && <p className="calendar-error">{drawError || loadError}</p>}
          <button type="button" className="calendar-cta" onClick={drawTodayFortune} disabled={drawing || Boolean(loadError)}>
            <span>{drawing ? "正在抽取..." : todayItem ? "查看今日签" : "签到并抽取今日运势"}</span>
            <Dices size={18} className={drawing ? "is-rolling" : ""} />
          </button>
        </aside>
      </section>

      <FortuneDialog open={dialogOpen} onOpenChange={setDialogOpen} item={selectedFortune} />
    </>
  );
}

export function FortuneDialog({ open, onOpenChange, item }) {
  if (!item) return null;
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fortune-dialog-overlay" />
        <Dialog.Content className="fortune-dialog" style={{ "--fortune-color": item.luckyColor || "#2563eb" }}>
          <Dialog.Close className="fortune-dialog-close" aria-label="关闭"><X size={18} /></Dialog.Close>
          <div className="fortune-orbit" aria-hidden="true"><Sparkles size={24} /></div>
          <span className="fortune-date">{item.date} · WHUT-ACM 今日签</span>
          <Dialog.Title>{item.fortuneTitle}</Dialog.Title>
          <Dialog.Description>{item.fortuneMessage}</Dialog.Description>
          <div className="fortune-details">
            <div><Tag size={16} /><span>幸运标签</span><strong>{item.luckyTag}</strong></div>
            <div>
              <Star size={16} />
              <span>今日灵感</span>
              <strong className="fortune-stars" aria-label={`${item.luckLevel} 星`}>
                {Array.from({ length: 5 }, (_, index) => <i key={index} className={index < item.luckLevel ? "is-lit" : ""} />)}
              </strong>
            </div>
          </div>
          <Dialog.Close className="fortune-dialog-confirm">收下今日好运</Dialog.Close>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function buildMonthCells(month, itemMap, today) {
  const year = month.getFullYear();
  const monthIndex = month.getMonth();
  const firstWeekday = (new Date(year, monthIndex, 1).getDay() + 6) % 7;
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
  const cells = Array(firstWeekday).fill(null);
  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = new Date(year, monthIndex, day);
    const key = formatDate(date);
    cells.push({ key, day, item: itemMap.get(key), today: sameDay(date, today), future: date > today });
  }
  while (cells.length % 7 !== 0) cells.push(null);
  return cells;
}

function buildSummary(itemMap, today, visibleMonth) {
  const activeKeys = [...itemMap.keys()].sort();
  const activeSet = new Set(activeKeys);
  const cursor = new Date(today);
  if (!activeSet.has(formatDate(cursor))) cursor.setDate(cursor.getDate() - 1);
  let streak = 0;
  while (activeSet.has(formatDate(cursor))) {
    streak += 1;
    cursor.setDate(cursor.getDate() - 1);
  }
  let longestStreak = 0;
  let currentRun = 0;
  let previous = null;
  activeKeys.forEach((key) => {
    const date = parseDate(key);
    const consecutive = previous && Math.round((date - previous) / 86400000) === 1;
    currentRun = consecutive ? currentRun + 1 : 1;
    longestStreak = Math.max(longestStreak, currentRun);
    previous = date;
  });
  const monthPrefix = `${visibleMonth.getFullYear()}-${String(visibleMonth.getMonth() + 1).padStart(2, "0")}-`;
  const monthActive = activeKeys.filter((key) => key.startsWith(monthPrefix)).length;
  const monday = new Date(today);
  monday.setDate(today.getDate() - ((today.getDay() + 6) % 7));
  let weekActive = 0;
  for (let offset = 0; offset < 7; offset += 1) {
    const date = new Date(monday);
    date.setDate(monday.getDate() + offset);
    if (date <= today && activeSet.has(formatDate(date))) weekActive += 1;
  }
  return { streak, longestStreak, monthActive, weekActive };
}

function startOfDay(date) { return new Date(date.getFullYear(), date.getMonth(), date.getDate()); }
function parseDate(value) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}
function formatDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
function sameDay(left, right) {
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth() && left.getDate() === right.getDate();
}
function sameMonth(left, right) {
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth();
}

export default memo(TrainingCalendar);
