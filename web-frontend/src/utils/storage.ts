const CLIENT_ID_KEY = 'client_id';

function randomId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `cid-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function getOrCreateClientId(): string {
  const saved = localStorage.getItem(CLIENT_ID_KEY);
  if (saved) {
    return saved;
  }
  const next = randomId();
  localStorage.setItem(CLIENT_ID_KEY, next);
  return next;
}

function lastSeenKey(sessionId: string) {
  return `last_seen:${sessionId}`;
}

export function getLastSeenMessageId(sessionId: string): string | null {
  return localStorage.getItem(lastSeenKey(sessionId));
}

export function setLastSeenMessageId(sessionId: string, messageId: string) {
  localStorage.setItem(lastSeenKey(sessionId), messageId);
}
