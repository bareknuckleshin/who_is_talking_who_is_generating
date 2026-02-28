const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000';

export type CreateSessionRequest = {
  topic: string;
  num_llm_speakers: number;
  turns_per_speaker: number;
  max_chars: number;
  language: string;
  difficulty: string;
};

export type CreateSessionResponse = {
  session_id: string;
  ws_url: string;
};

export async function createSession(payload: CreateSessionRequest): Promise<CreateSessionResponse> {
  const response = await fetch(`${API_BASE_URL}/sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  return (await response.json()) as CreateSessionResponse;
}

export function getApiBaseUrl() {
  return API_BASE_URL;
}
