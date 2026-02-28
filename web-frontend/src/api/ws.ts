import type { InboundEvent, OutboundEvent } from '../types/events';

export type WsClientOptions = {
  url: string;
  getJoinPayload: () => OutboundEvent;
  onEvent: (event: InboundEvent) => void;
  onOpen?: () => void;
  onClose?: () => void;
  onConnectionState?: (state: { connected: boolean; reconnecting: boolean; lastError: string | null }) => void;
};

export class SessionWsClient {
  private ws: WebSocket | null = null;
  private retryIndex = 0;
  private reconnectTimer: number | null = null;
  private closedByUser = false;
  private readonly backoffMs = [1000, 2000, 5000];

  constructor(private readonly opts: WsClientOptions) {}

  connect() {
    this.closedByUser = false;
    this.openSocket();
  }

  close() {
    this.closedByUser = true;
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.ws?.close();
    this.ws = null;
  }

  send(event: OutboundEvent) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return;
    }
    this.ws.send(JSON.stringify(event));
  }

  private openSocket() {
    this.opts.onConnectionState?.({ connected: false, reconnecting: this.retryIndex > 0, lastError: null });
    this.ws = new WebSocket(this.opts.url);

    this.ws.onopen = () => {
      this.retryIndex = 0;
      this.opts.onConnectionState?.({ connected: true, reconnecting: false, lastError: null });
      this.send(this.opts.getJoinPayload());
      this.opts.onOpen?.();
    };

    this.ws.onmessage = (message) => {
      try {
        const parsed = JSON.parse(message.data) as InboundEvent;
        if (!parsed || typeof parsed.type !== 'string') {
          console.warn('Unknown event payload');
          return;
        }
        this.opts.onEvent(parsed);
      } catch (error) {
        console.warn('Invalid WS message JSON:', error);
      }
    };

    this.ws.onerror = () => {
      this.opts.onConnectionState?.({ connected: false, reconnecting: true, lastError: 'WebSocket error' });
    };

    this.ws.onclose = () => {
      this.opts.onClose?.();
      this.ws = null;
      if (this.closedByUser) {
        this.opts.onConnectionState?.({ connected: false, reconnecting: false, lastError: null });
        return;
      }
      this.scheduleReconnect();
    };
  }

  private scheduleReconnect() {
    const delay = this.backoffMs[Math.min(this.retryIndex, this.backoffMs.length - 1)];
    this.opts.onConnectionState?.({ connected: false, reconnecting: true, lastError: `Reconnecting in ${delay / 1000}s` });
    this.reconnectTimer = window.setTimeout(() => {
      this.retryIndex += 1;
      this.openSocket();
    }, delay);
  }
}
