import type { SessionStatus } from '../types/models';

type Props = {
  topic: string;
  status: SessionStatus;
  participantsCount: number;
  isMyTurn: boolean;
};

export default function HeaderBar({ topic, status, participantsCount, isMyTurn }: Props) {
  return (
    <header className="header-bar">
      <div>
        <h2>{topic || 'Who-is-Human'}</h2>
        <p>Status: {status}</p>
      </div>
      <div className="header-meta">
        <span>참가자: {participantsCount}명</span>
        <span>{isMyTurn ? '내 차례야' : '상대 차례야'}</span>
      </div>
    </header>
  );
}
