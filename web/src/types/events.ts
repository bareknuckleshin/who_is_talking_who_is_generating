export type SessionStateEvent = {
  type: 'session.state';
  topic: string;
  status: 'LOBBY' | 'IN_PROGRESS' | 'JUDGING' | 'FINISHED';
  participants: Array<{ seat: string }>;
  turn_counts: Record<string, number>;
  current_speaker_seat: string | null;
  max_chars: number;
};

export type TurnRequestHumanEvent = {
  type: 'turn.request_human';
  timeout_secs?: number;
};

export type MessageTypingEvent = {
  type: 'message.typing';
  seat: string;
};

export type MessageNewEvent = {
  type: 'message.new';
  message_id: string;
  turn_index: number;
  seat: string;
  text: string;
  ts?: string;
};

export type TurnNextEvent = {
  type: 'turn.next';
  current_speaker_seat: string | null;
  turn_counts?: Record<string, number>;
};

export type SessionFinishedEvent = {
  type: 'session.finished';
  pick_seat: string;
  confidence: number;
  why: string;
};

export type InboundEvent =
  | SessionStateEvent
  | TurnRequestHumanEvent
  | MessageTypingEvent
  | MessageNewEvent
  | TurnNextEvent
  | SessionFinishedEvent;

export type SessionJoinEvent = {
  type: 'session.join';
  client_id: string;
};

export type SessionResumeEvent = {
  type: 'session.resume';
  client_id: string;
  last_seen_message_id: string | null;
};

export type HumanMessageEvent = {
  type: 'human.message';
  client_id: string;
  text: string;
};

export type SessionRequestStateEvent = {
  type: 'session.request_state';
  client_id: string;
};

export type OutboundEvent =
  | SessionJoinEvent
  | SessionResumeEvent
  | HumanMessageEvent
  | SessionRequestStateEvent;
