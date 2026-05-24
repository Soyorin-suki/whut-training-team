/**
 * GitHub-style contribution heatmap — 365 days.
 * data: [{ date: "2026-05-23", score: 1500, colorLevel: 2 }, ...]
 */
const COLOR_CLASSES = [
  "bg-[#ebedf0]",    // level 0 - no activity
  "bg-[#9be9a8]",    // level 1
  "bg-[#40c463]",    // level 2
  "bg-[#30a14e]",    // level 3
  "bg-[#216e39]",    // level 4
];

const CELL_SIZE = 11;
const CELL_GAP = 2;
const WEEK_WIDTH = CELL_SIZE + CELL_GAP; // 13px per week column
const DAY_LABEL_WIDTH = 28; // matches ml-7 in grid row

const MONTH_NAMES = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
const DAY_NAMES = ["", "Mon", "", "Wed", "", "Fri", ""];

export default function Heatmap({ data }) {
  // Build a map of date -> item
  const dateMap = {};
  if (data && data.length > 0) {
    for (const item of data) {
      dateMap[item.date] = item;
    }
  }

  // Generate the last 365 days
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const endDate = new Date(today);
  const startDate = new Date(today);
  startDate.setDate(startDate.getDate() - 365);
  // Align start to previous Sunday
  startDate.setDate(startDate.getDate() - startDate.getDay());

  // Save for month calculation
  const startDateForMonths = new Date(startDate);

  // Build week grid — stop generating days beyond endDate
  const weeks = [];
  const current = new Date(startDate);
  while (current <= endDate) {
    const week = [];
    for (let d = 0; d < 7 && current <= endDate; d++) {
      const dateStr = formatDate(current);
      const item = dateMap[dateStr];
      week.push({
        date: dateStr,
        level: item ? item.colorLevel : 0,
        score: item ? item.score : 0,
      });
      current.setDate(current.getDate() + 1);
    }
    // Pad short last week to 7 slots (empty, not rendered)
    while (week.length < 7) {
      week.push(null);
    }
    weeks.push(week);
  }

  // Month spans — how many weeks each month covers
  const monthSpans = [];
  for (let w = 0; w < weeks.length; ) {
    const midDate = new Date(startDateForMonths);
    midDate.setDate(midDate.getDate() + w * 7 + 3);
    const m = midDate.getMonth();
    let count = 0;
    while (w + count < weeks.length) {
      const checkDate = new Date(startDateForMonths);
      checkDate.setDate(checkDate.getDate() + (w + count) * 7 + 3);
      if (checkDate.getMonth() !== m) break;
      count++;
    }
    if (count === 0) count = 1;
    monthSpans.push({ month: m, weeks: count });
    w += count;
  }

  return (
    <div className="inline-block overflow-x-auto max-w-full">
      {/* Month labels — aligned with grid via spacer */}
      <div className="flex mb-1 text-[10px] text-text-secondary" style={{ gap: CELL_GAP }}>
        <div style={{ width: DAY_LABEL_WIDTH, flexShrink: 0 }} />
        {monthSpans.map((ms, i) => (
          <span
            key={i}
            className="whitespace-nowrap overflow-visible"
            style={{ width: ms.weeks * WEEK_WIDTH - CELL_GAP, minWidth: 28 }}
          >
            {MONTH_NAMES[ms.month]}
          </span>
        ))}
      </div>

      <div className="flex">
        {/* Day labels column */}
        <div className="flex flex-col flex-shrink-0" style={{ width: DAY_LABEL_WIDTH, gap: CELL_GAP, paddingRight: CELL_GAP }}>
          {DAY_NAMES.map((d, i) => (
            <span key={i} className="text-[9px] text-text-secondary" style={{ lineHeight: `${CELL_SIZE}px`, height: CELL_SIZE }}>
              {d}
            </span>
          ))}
        </div>

        {/* Grid */}
        <div className="flex" style={{ gap: CELL_GAP }}>
          {weeks.map((week, wi) => (
            <div key={wi} className="flex flex-col" style={{ gap: CELL_GAP }}>
              {week.map((day, di) => (
                <div
                  key={di}
                  style={{ width: CELL_SIZE, height: CELL_SIZE }}
                  className={`rounded-[2px] ${
                    day ? (day.level >= 0 ? COLOR_CLASSES[day.level] : "bg-transparent") : "bg-transparent"
                  }`}
                  title={day ? `${day.date}: ${day.score} 分` : ""}
                />
              ))}
            </div>
          ))}
        </div>
      </div>

      {/* Legend */}
      <div className="flex items-center justify-end gap-1 mt-1.5">
        <span className="text-[9px] text-text-secondary">Less</span>
        {COLOR_CLASSES.map((cls, i) => (
          <div key={i} style={{ width: CELL_SIZE, height: CELL_SIZE }} className={`rounded-[2px] ${cls}`} />
        ))}
        <span className="text-[9px] text-text-secondary">More</span>
      </div>
    </div>
  );
}

function formatDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}
