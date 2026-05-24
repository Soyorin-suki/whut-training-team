export function getRatingMeta(rating) {
  if (rating === null || rating === undefined) return { label: "Unrated", color: "#666666" };
  if (rating < 1200) return { label: "Newbie", color: "#808080" };
  if (rating < 1400) return { label: "Pupil", color: "#008000" };
  if (rating < 1600) return { label: "Specialist", color: "#03a89e" };
  if (rating < 1900) return { label: "Expert", color: "#0000ff" };
  if (rating < 2100) return { label: "Candidate Master", color: "#aa00aa" };
  if (rating < 2400) return { label: "Master", color: "#ff8c00" };
  return { label: "Grandmaster", color: "#ff0000" };
}

export function parseTags(tags) {
  if (!tags) return [];
  return String(tags)
    .split(",")
    .map((t) => t.trim())
    .filter(Boolean)
    .slice(0, 6);
}
