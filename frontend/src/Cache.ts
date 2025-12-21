// Simple in-memory cache for API responses, now using sessionStorage for persistence across reloads
const CACHE_EXPIRATION_MS = 10 * 60 * 1000; // 10 minutes

function getCacheObj(): Record<string, { data: unknown, timestamp: number }> {
  const raw = sessionStorage.getItem('apiCache');
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch {
    return {};
  }
}

function setCacheObj(obj: Record<string, { data: unknown, timestamp: number }>) {
  sessionStorage.setItem('apiCache', JSON.stringify(obj));
}

export function getCached(key: string) {
  const cache = getCacheObj();
  const entry = cache[key];
  if (!entry) return undefined;
  if (Date.now() - entry.timestamp > CACHE_EXPIRATION_MS) {
    delete cache[key];
    setCacheObj(cache);
    return undefined;
  }
  return entry.data;
}

export function setCached(key: string, data: unknown) {
  const cache = getCacheObj();
  cache[key] = { data, timestamp: Date.now() };
  setCacheObj(cache);
}

export function clearCache() {
  sessionStorage.removeItem('apiCache');
}
