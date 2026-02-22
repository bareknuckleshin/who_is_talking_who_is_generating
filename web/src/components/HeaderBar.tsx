import type { SessionStatus } from '../types/models';

type Props = {
  topic: string;
  status: SessionStatus;
  seats: string[];
  currentSpeakerSeat: string | null;
};

export default function HeaderBar({ topic, status, seats, currentSpeakerSeat }: Props) {
  return (
    <header className="header-bar">
      <div>
        <h2>{topic || 'Who-is-Human'}</h2>
        <p>Status: {status}</p>
      </div>
      <div className="seat-list">
        {seats.map((seat) => (
          <span key={seat} className={`seat-chip ${currentSpeakerSeat === seat ? 'active' : ''}`}>
            {seat}
          </span>
        ))}
      </div>
    </header>
  );
}
