import { expandPalette, type ThemePayload } from './expand';
import { renderTheme, renderClearState } from './render';

declare const cast: any;

const NAMESPACE = 'urn:x-cast:me.mmckenna.halogen';
const RECEIVER_MAX_V = 1;

interface Ack {
  type: 'ack';
  v: number;
  id: string;
  ts: number;
  deviceName: string;
  negotiatedVersion: number;
  ok: boolean;
  error?: string;
}

const context = cast.framework.CastReceiverContext.getInstance();
const options = new cast.framework.CastReceiverOptions();
options.customNamespaces = { [NAMESPACE]: cast.framework.system.MessageType.JSON };

const bus = context.getCustomMessageBus(NAMESPACE);

bus.addEventListener(
  cast.framework.system.EventType.MESSAGE,
  (event: { senderId: string; data: unknown }) => {
    const raw = typeof event.data === 'string' ? event.data : JSON.stringify(event.data);
    let parsed: any;
    try {
      parsed = typeof event.data === 'string' ? JSON.parse(raw) : event.data;
    } catch {
      reply(event.senderId, ack(0, 'parse-error', false, 'invalid_json'));
      return;
    }

    if (typeof parsed?.v === 'number' && parsed.v > RECEIVER_MAX_V) {
      reply(event.senderId, ack(parsed.v, parsed.id, false, 'unsupported_version'));
      return;
    }

    switch (parsed?.type) {
      case 'handshake':
        reply(event.senderId, ack(parsed.v, parsed.id, true));
        break;
      case 'theme':
        applyTheme(parsed as ThemePayload);
        reply(event.senderId, ack(parsed.v, parsed.id, true));
        break;
      case 'clear':
        renderClearState();
        reply(event.senderId, ack(parsed.v, parsed.id, true));
        break;
      default:
        reply(event.senderId, ack(parsed?.v ?? 1, parsed?.id ?? 'unknown', false, 'unknown_type'));
    }
  },
);

function applyTheme(payload: ThemePayload): void {
  const { light, dark } = expandPalette(payload);
  // Start on the light scheme; toggle to dark on a 6s interval to showcase both.
  renderTheme(light, payload);
  let showingDark = false;
  clearInterval((applyTheme as any)._t);
  (applyTheme as any)._t = setInterval(() => {
    showingDark = !showingDark;
    renderTheme(showingDark ? dark : light, payload);
  }, 6_000);
}

function ack(v: number, id: string, ok: boolean, error?: string): Ack {
  return {
    type: 'ack',
    v,
    id,
    ts: Date.now(),
    deviceName: context.getDeviceCapabilities?.()?.friendlyName ?? 'Chromecast',
    negotiatedVersion: Math.min(v, RECEIVER_MAX_V),
    ok,
    ...(error ? { error } : {}),
  };
}

function reply(senderId: string, msg: Ack): void {
  bus.sendMessage(senderId, JSON.stringify(msg));
}

context.start(options);
