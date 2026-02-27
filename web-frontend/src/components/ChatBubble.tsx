import type { Message } from '../types/models';

type Props = {
  message: Message;
  isHumanSeat: boolean;
};

export default function ChatBubble({ message, isHumanSeat }: Props) {
  return (
    <div className={`bubble-row ${isHumanSeat ? 'right' : 'left'}`}>
      <div className={`chat-bubble ${isHumanSeat ? 'human' : 'llm'}`}>
        <div className="meta">
          <span className="seat-label">{message.seat}</span>
          <span>#{message.turnIndex}</span>
        </div>
        <p>{message.text}</p>
      </div>
    </div>
  );
}
