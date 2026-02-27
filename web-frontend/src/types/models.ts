export type SessionStatus = 'LOBBY' | 'IN_PROGRESS' | 'JUDGING' | 'FINISHED';

export type SessionState = {
  sessionId: string;
  topic: string;
  status: SessionStatus;
  participants: string[];
  turnCounts: Record<string, number>;
  currentSpeakerSeat: string | null;
  maxChars: number;
};

export type Message = {
  messageId: string;
  turnIndex: number;
  seat: string;
  text: string;
  ts?: string;
};

export type TypingState = Record<string, boolean>;

export type ConnectionState = {
  connected: boolean;
  reconnecting: boolean;
  lastError: string | null;
};

export type JudgeResult = {
  pick_seat: string;
  confidence: number;
  why: string;
};
