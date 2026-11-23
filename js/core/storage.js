// Универсальные безопасные помощники localStorage для будущего модульного кода.
export function readJSON(key, fallback = null) {
  try { const raw = localStorage.getItem(key); return raw === null ? fallback : JSON.parse(raw); }
  catch { return fallback; }
}
export function writeJSON(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)); return true; }
  catch { return false; }
}
