import type { JudgeResult } from '../types/models';

type Props = {
  result: JudgeResult | null;
  onClose: () => void;
};

export default function JudgeModal({ result, onClose }: Props) {
  if (!result) {
    return null;
  }

  return (
    <div className="judge-backdrop">
      <div className="judge-modal">
        <h3>Judge Result</h3>
        <p>
          <strong>PICK:</strong> {result.pick_seat}
        </p>
        <p>
          <strong>CONF:</strong> {result.confidence}
        </p>
        <p>
          <strong>WHY:</strong> {result.why}
        </p>
        <button onClick={onClose}>닫기</button>
      </div>
    </div>
  );
}
