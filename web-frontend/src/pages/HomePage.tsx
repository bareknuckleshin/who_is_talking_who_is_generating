import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createSession } from '../api/http';

export default function HomePage() {
  const navigate = useNavigate();
  const [topic, setTopic] = useState('');
  const [numLlmSpeakers, setNumLlmSpeakers] = useState(1);
  const [turnsPerSpeaker, setTurnsPerSpeaker] = useState(5);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onCreate = async () => {
    if (!topic.trim()) {
      setError('Topic은 필수입니다.');
      return;
    }

    setError(null);
    setLoading(true);
    try {
      const response = await createSession({
        topic: topic.trim(),
        num_llm_speakers: numLlmSpeakers,
        turns_per_speaker: turnsPerSpeaker,
        max_chars: 160,
        language: 'ko',
        difficulty: 'normal',
      });
      navigate(`/s/${response.session_id}`, {
        state: {
          wsUrlPath: response.ws_url,
          topic: topic.trim(),
        },
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : '세션 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="page page-home">
      <h1>Who-is-Human</h1>
      <div className="form-card">
        <label>
          Topic
          <input value={topic} onChange={(e) => setTopic(e.target.value)} required />
        </label>
        <label>
          num_llm_speakers
          <input
            type="number"
            value={numLlmSpeakers}
            min={1}
            max={5}
            onChange={(e) => setNumLlmSpeakers(Number(e.target.value))}
          />
        </label>
        <label>
          turns_per_speaker
          <input
            type="number"
            value={turnsPerSpeaker}
            min={1}
            max={10}
            onChange={(e) => setTurnsPerSpeaker(Number(e.target.value))}
          />
        </label>
        <p>max_chars: 160 (fixed)</p>
        <button onClick={onCreate} disabled={loading}>
          {loading ? 'Creating...' : 'Create'}
        </button>
        {error && <p className="error">{error}</p>}
      </div>
    </main>
  );
}
