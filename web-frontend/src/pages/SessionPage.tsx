import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { getApiBaseUrl } from '../api/http';
import { SessionWsClient } from '../api/ws';
import ChatInput from '../components/ChatInput';
import ChatLog from '../components/ChatLog';
import HeaderBar from '../components/HeaderBar';
import JudgeModal from '../components/JudgeModal';
import type { InboundEvent } from '../types/events';
import type { ConnectionState, JudgeResult, Message, SessionState, TypingState } from '../types/models';
import { getLastSeenMessageId, getOrCreateClientId, setLastSeenMessageId } from '../utils/storage';

const HUMAN_SEAT = 'A';

const defaultState = (sessionId: string): SessionState => ({
  sessionId,
  topic: '',
  status: 'LOBBY',
  participants: [],
  turnCounts: {},
  currentSpeakerSeat: null,
  maxChars: 160,
});

function toWsBaseUrl(apiBaseUrl: string) {
  if (apiBaseUrl.startsWith('https://')) {
    return apiBaseUrl.replace('https://', 'wss://');
  }
  return apiBaseUrl.replace('http://', 'ws://');
}

export default function SessionPage() {
  const { sessionId = '' } = useParams();
  const location = useLocation();
  const wsClientRef = useRef<SessionWsClient | null>(null);
  const countdownTimerRef = useRef<number | null>(null);

  const [sessionState, setSessionState] = useState<SessionState>(defaultState(sessionId));
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageIds, setMessageIds] = useState<Set<string>>(new Set());
  const [typingState, setTypingState] = useState<TypingState>({});
  const [input, setInput] = useState('');
  const [judgeResult, setJudgeResult] = useState<JudgeResult | null>(null);
  const [connectionState, setConnectionState] = useState<ConnectionState>({
    connected: false,
    reconnecting: false,
    lastError: null,
  });
  const [countdownSecs, setCountdownSecs] = useState<number | null>(null);

  const clientId = useMemo(() => getOrCreateClientId(), []);
  const wsPathFromHome = (location.state as { wsUrlPath?: string } | null)?.wsUrlPath;

  useEffect(() => {
    const wsPath = wsPathFromHome ?? `/ws/sessions/${sessionId}`;
    const wsUrl = `${toWsBaseUrl(getApiBaseUrl())}${wsPath}`;

    const client = new SessionWsClient({
      url: wsUrl,
      getJoinPayload: () => {
        const lastSeen = getLastSeenMessageId(sessionId);
        if (lastSeen) {
          return {
            type: 'session.resume',
            client_id: clientId,
            last_seen_message_id: lastSeen,
          };
        }
        return {
          type: 'session.join',
          client_id: clientId,
        };
      },
      onEvent: (event) => handleEvent(event),
      onConnectionState: setConnectionState,
    });

    wsClientRef.current = client;
    client.connect();

    return () => {
      client.close();
      if (countdownTimerRef.current) {
        window.clearInterval(countdownTimerRef.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId]);

  const startCountdown = (secs: number) => {
    if (countdownTimerRef.current) {
      window.clearInterval(countdownTimerRef.current);
    }
    setCountdownSecs(secs);
    countdownTimerRef.current = window.setInterval(() => {
      setCountdownSecs((prev) => {
        if (prev === null || prev <= 1) {
          if (countdownTimerRef.current) {
            window.clearInterval(countdownTimerRef.current);
            countdownTimerRef.current = null;
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleEvent = (event: InboundEvent) => {
    switch (event.type) {
      case 'session.state':
        setSessionState((prev) => ({
          ...prev,
          topic: event.topic,
          status: event.status,
          participants: event.participants.map((p) => p.seat),
          turnCounts: event.turn_counts,
          currentSpeakerSeat: event.current_speaker_seat,
          maxChars: event.max_chars,
        }));
        return;
      case 'turn.request_human':
        if (event.timeout_secs) {
          startCountdown(event.timeout_secs);
        }
        return;
      case 'message.typing':
        if (event.seat === HUMAN_SEAT) {
          return;
        }
        setTypingState((prev) => ({ ...prev, [event.seat]: true }));
        return;
      case 'message.new':
        setTypingState((prev) => ({ ...prev, [event.seat]: false }));
        setMessageIds((prev) => {
          if (prev.has(event.message_id)) {
            return prev;
          }
          const copy = new Set(prev);
          copy.add(event.message_id);
          setMessages((old) => [
            ...old,
            {
              messageId: event.message_id,
              turnIndex: event.turn_index,
              seat: event.seat,
              text: event.text,
              ts: event.ts,
            },
          ]);
          setLastSeenMessageId(sessionId, event.message_id);
          return copy;
        });
        return;
      case 'turn.next':
        setSessionState((prev) => ({
          ...prev,
          currentSpeakerSeat: event.current_speaker_seat,
          turnCounts: event.turn_counts ?? prev.turnCounts,
        }));
        return;
      case 'session.finished':
        setSessionState((prev) => ({ ...prev, status: 'FINISHED' }));
        setJudgeResult({
          pick_seat: event.pick_seat,
          confidence: event.confidence,
          why: event.why,
        });
        return;
      default:
        console.warn('Unknown event type:', event);
    }
  };

  const isMyTurn = sessionState.currentSpeakerSeat === HUMAN_SEAT;
  const turnHintText = isMyTurn ? '메시지 입력' : '상대 발화 중...';

  const sendHumanMessage = () => {
    const text = input.trim();
    if (!isMyTurn || !text) {
      return;
    }
    wsClientRef.current?.send({
      type: 'human.message',
      client_id: clientId,
      text,
    });
    setInput('');
  };

  return (
    <main className="page page-session">
      <HeaderBar
        topic={sessionState.topic}
        status={sessionState.status}
        participantsCount={sessionState.participants.length}
        isMyTurn={isMyTurn}
      />
      {connectionState.reconnecting && <div className="banner">재연결 중...</div>}
      {connectionState.lastError && <div className="banner error">{connectionState.lastError}</div>}
      <ChatLog messages={messages} typingState={typingState} />
      <ChatInput
        value={input}
        maxChars={sessionState.maxChars}
        enabled={isMyTurn}
        onChange={setInput}
        onSend={sendHumanMessage}
        hintText={turnHintText}
        countdownSecs={countdownSecs}
      />
      <JudgeModal result={judgeResult} onClose={() => setJudgeResult(null)} />
    </main>
  );
}
