import type { RoleMap, ThemePayload } from './expand';

const FONT_MAP: Record<string, string> = {
  modern: 'Inter, system-ui, sans-serif',
  classic: 'Georgia, "Times New Roman", serif',
  playful: '"Baloo 2", "Comic Sans MS", cursive',
  minimal: '"Helvetica Neue", Helvetica, Arial, sans-serif',
  monospace: '"JetBrains Mono", "SF Mono", ui-monospace, monospace',
};

const CORNER_MAP: Record<string, number> = {
  sharp: 2,
  rounded: 16,
  pill: 999,
  soft: 10,
};

/**
 * Paint the full role grid + sample UI from the expanded [roles] and the
 * original [payload] (for typography/shape hints and sender metadata).
 */
export function renderTheme(roles: RoleMap, payload: ThemePayload): void {
  applyCssVars(roles, payload);
  fillRoleGrid(roles);
  updateHeader(payload);
  pulse();
}

export function renderClearState(): void {
  const sub = document.getElementById('subtitle');
  if (sub) sub.textContent = 'Cleared — waiting for a theme…';
  const meta = document.getElementById('sender-meta');
  if (meta) meta.textContent = '';
  const grid = document.getElementById('role-grid');
  if (grid) grid.innerHTML = '';
}

function applyCssVars(roles: RoleMap, p: ThemePayload): void {
  const r = document.documentElement.style;
  for (const [name, value] of Object.entries(roles)) {
    r.setProperty(`--halogen-${kebab(name)}`, value);
  }
  r.setProperty('--halogen-corner', `${CORNER_MAP[p.cs ?? 'rounded'] ?? 16 * (p.cx ?? 1)}px`);
  r.setProperty('--halogen-heading-weight', String(p.hw ?? 600));
  r.setProperty('--halogen-body-weight', String(p.bw ?? 400));
  r.setProperty('--halogen-letter-spacing', p.ls ? '-0.02em' : 'normal');
  r.setProperty('--halogen-font-family', FONT_MAP[p.font ?? 'modern'] ?? FONT_MAP.modern!);
}

function fillRoleGrid(roles: RoleMap): void {
  const grid = document.getElementById('role-grid');
  if (!grid) return;
  grid.innerHTML = '';
  for (const [name, hex] of Object.entries(roles)) {
    const cell = document.createElement('div');
    cell.className = 'role-cell';
    cell.style.background = hex;
    cell.style.color = contrastText(hex);
    cell.innerHTML =
      `<span class="name">${escapeHtml(name)}</span>` +
      `<span class="hex">${escapeHtml(hex)}</span>`;
    grid.appendChild(cell);
  }
}

function updateHeader(p: ThemePayload): void {
  const sub = document.getElementById('subtitle');
  if (sub) sub.textContent = p.label ?? p.key;
  const meta = document.getElementById('sender-meta');
  if (meta) {
    const s = p.sender;
    meta.textContent = s
      ? `${s.appId} ${s.appVersion} · ${s.platform} · halogen ${s.halogenVersion}`
      : `key ${p.key}`;
  }
}

function pulse(): void {
  const app = document.getElementById('app');
  if (!app) return;
  app.classList.remove('pulse');
  // Force reflow so the animation restarts on every new theme.
  void app.offsetWidth;
  app.classList.add('pulse');
}

function kebab(s: string): string {
  return s.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
}

function contrastText(hex: string): string {
  const h = hex.replace('#', '');
  const r = parseInt(h.slice(0, 2), 16);
  const g = parseInt(h.slice(2, 4), 16);
  const b = parseInt(h.slice(4, 6), 16);
  const luma = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
  return luma > 0.55 ? '#111111' : '#FFFFFF';
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}
