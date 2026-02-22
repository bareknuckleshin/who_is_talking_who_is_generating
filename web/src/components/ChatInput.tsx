import { useMemo } from 'react';
import { clampText, countChars } from '../utils/text';

type Props = {
  value: string;
  maxChars: number;
  enabled: boolean;
  onChange: (next: string) => void;
  onSend: () => void;
  hintText: string;
  countdownSecs: number | null;
};

export default function ChatInput({ value, maxChars, enabled, onChange, onSend, hintText, countdownSecs }: Props) {
  const chars = useMemo(() => countChars(value), [value]);

  return (
    <div className="chat-input-wrap">
      <input
        value={value}
        maxLength={maxChars}
        placeholder={hintText}
        disabled={!enabled}
        onChange={(event) => onChange(clampText(event.target.value, maxChars))}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault();
            onSend();
          }
        }}
      />
      <button onClick={onSend} disabled={!enabled || !value.trim()}>
        Send
      </button>
      <div className="chat-input-meta">
        <span>
          {chars}/{maxChars}
        </span>
        {enabled && countdownSecs !== null && <span>남은 시간: {countdownSecs}s</span>}
      </div>
    </div>
  );
}
