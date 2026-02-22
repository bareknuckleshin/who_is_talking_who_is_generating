import { useEffect, useRef } from 'react';
import type { Message, TypingState } from '../types/models';
import ChatBubble from './ChatBubble';

type Props = {
  messages: Message[];
  typingState: TypingState;
};

export default function ChatLog({ messages, typingState }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, typingState]);

  return (
    <div className="chat-log">
      {messages.map((message) => (
        <ChatBubble key={message.messageId} message={message} isHumanSeat={message.seat === 'A'} />
      ))}
      {Object.entries(typingState)
        .filter(([, isTyping]) => isTyping)
        .map(([seat]) => (
          <div key={seat} className="typing-indicator">
            {seat} 입력 중...
          </div>
        ))}
      <div ref={bottomRef} />
    </div>
  );
}
