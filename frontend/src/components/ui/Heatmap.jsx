import { memo } from "react";

const LEVEL_COLORS = ["#ebedf0", "#9be9a8", "#40c463", "#30a14e", "#216e39"];
const CELL_SIZE = 16;
const CELL_GAP = 3;
const WEEK_WIDTH = CELL_SIZE + CELL_GAP;
const DAY_LABEL_WIDTH = 34;
const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const DAY_NAMES = ["", "Mon", "", "Wed", "", "Fri", ""];

function Heatmap({ data }) {
  const dateMap = Object.fromEntries((data || []).map((item) => [item.date, item]));
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const startDate = new Date(today);
  startDate.setDate(startDate.getDate() - 365);
  startDate.setDate(startDate.getDate() - startDate.getDay());
  const startDateForMonths = new Date(startDate);

  const weeks = [];
  const current = new Date(startDate);
  while (current <= today) {
    const week = [];
    for (let dayIndex = 0; dayIndex < 7 && current <= today; dayIndex += 1) {
      const date = formatDate(current);
      const item = dateMap[date];
      week.push({
        date,
        level: Math.max(0, Math.min(4, item?.colorLevel ?? 0)),
        score: item?.score ?? 0,
      });
      current.setDate(current.getDate() + 1);
    }
    while (week.length < 7) week.push(null);
    weeks.push(week);
  }

  const monthSpans = [];
  for (let weekIndex = 0; weekIndex < weeks.length;) {
    const midDate = new Date(startDateForMonths);
    midDate.setDate(midDate.getDate() + weekIndex * 7 + 3);
    const month = midDate.getMonth();
    let count = 0;
    while (weekIndex + count < weeks.length) {
      const checkDate = new Date(startDateForMonths);
      checkDate.setDate(checkDate.getDate() + (weekIndex + count) * 7 + 3);
      if (checkDate.getMonth() !== month) break;
      count += 1;
    }
    monthSpans.push({ month, weeks: Math.max(count, 1) });
    weekIndex += Math.max(count, 1);
  }

  const activeDays = (data || []).filter((item) => (item.score ?? 0) > 0).length;
  const totalScore = (data || []).reduce((sum, item) => sum + (item.score ?? 0), 0);

  return (
    <div className="heatmap-frame">
      <div className="heatmap-summary">
        <div>
          <span>ACTIVE DAYS</span>
          <strong>{activeDays}</strong>
        </div>
        <div>
          <span>TOTAL SCORE</span>
          <strong>{totalScore.toLocaleString()}</strong>
        </div>
        <p>过去 365 天的训练轨迹</p>
      </div>

      <div className="heatmap-scroll">
        <div
          className="heatmap-canvas"
          style={{ minWidth: DAY_LABEL_WIDTH + weeks.length * WEEK_WIDTH }}
        >
          <div className="heatmap-months" style={{ gap: CELL_GAP }}>
            <div style={{ width: DAY_LABEL_WIDTH, flexShrink: 0 }} />
            {monthSpans.map((span, index) => (
              <span
                key={`${span.month}-${index}`}
                style={{ width: span.weeks * WEEK_WIDTH - CELL_GAP }}
              >
                {MONTH_NAMES[span.month]}
              </span>
            ))}
          </div>

          <div className="heatmap-body">
            <div
              className="heatmap-days"
              style={{ width: DAY_LABEL_WIDTH, gap: CELL_GAP, paddingRight: CELL_GAP }}
            >
              {DAY_NAMES.map((day, index) => (
                <span
                  key={index}
                  style={{ lineHeight: `${CELL_SIZE}px`, height: CELL_SIZE }}
                >
                  {day}
                </span>
              ))}
            </div>

            <div className="heatmap-weeks" style={{ gap: CELL_GAP }}>
              {weeks.map((week, weekIndex) => (
                <div key={weekIndex} className="heatmap-week" style={{ gap: CELL_GAP }}>
                  {week.map((day, dayIndex) => (
                    <span
                      key={dayIndex}
                      className={`heatmap-cell ${day ? "" : "is-empty"}`}
                      style={{
                        width: CELL_SIZE,
                        height: CELL_SIZE,
                        background: day ? LEVEL_COLORS[day.level] : "transparent",
                      }}
                      title={day ? `${day.date} · ${day.score} 分` : ""}
                      aria-label={day ? `${day.date}，${day.score} 分` : undefined}
                    />
                  ))}
                </div>
              ))}
            </div>
          </div>

          <div className="heatmap-legend">
            <span>LESS</span>
            {LEVEL_COLORS.map((color) => (
              <i key={color} style={{ width: CELL_SIZE, height: CELL_SIZE, background: color }} />
            ))}
            <span>MORE</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default memo(Heatmap);

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
