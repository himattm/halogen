import {
  argbFromHex,
  hexFromArgb,
  Hct,
  TonalPalette,
} from '@material/material-color-utilities';

export interface ThemePayload {
  type: 'theme';
  v: number;
  id: string;
  ts: number;
  key: string;
  pri: string;
  sec?: string;
  ter?: string;
  neuL?: string;
  neuD?: string;
  err?: string;
  font?: string;
  hw?: number;
  bw?: number;
  ls?: boolean;
  cs?: string;
  cx?: number;
  label?: string;
  sender?: {
    appId: string;
    appVersion: string;
    platform: string;
    halogenVersion: string;
  };
}

export type RoleMap = Record<string, string>;

/**
 * Expand the six compact Halogen seed colors into the full Material 3 role set
 * (both light and dark). Missing seed colors are derived from `pri` via hue
 * rotation, matching the behavior of halogen-core's ThemeExpander.
 */
export function expandPalette(payload: ThemePayload): { light: RoleMap; dark: RoleMap } {
  const primary = TonalPalette.fromInt(argbFromHex(payload.pri));
  const secondary = payload.sec
    ? TonalPalette.fromInt(argbFromHex(payload.sec))
    : rotateHue(primary, -60);
  const tertiary = payload.ter
    ? TonalPalette.fromInt(argbFromHex(payload.ter))
    : rotateHue(primary, 60);

  const neutralLight = payload.neuL
    ? TonalPalette.fromInt(argbFromHex(payload.neuL))
    : TonalPalette.fromHueAndChroma(Hct.fromInt(primary.tone(50)).hue, 4);
  const neutralDark = payload.neuD
    ? TonalPalette.fromInt(argbFromHex(payload.neuD))
    : neutralLight;

  const err = payload.err
    ? TonalPalette.fromInt(argbFromHex(payload.err))
    : TonalPalette.fromHueAndChroma(25, 84);

  return {
    light: roleMap(primary, secondary, tertiary, neutralLight, err, /*dark*/ false),
    dark: roleMap(primary, secondary, tertiary, neutralDark, err, /*dark*/ true),
  };
}

function rotateHue(base: TonalPalette, degrees: number): TonalPalette {
  const anchorHct = Hct.fromInt(base.tone(50));
  const hue = (anchorHct.hue + degrees + 360) % 360;
  return TonalPalette.fromHueAndChroma(hue, Math.max(20, anchorHct.chroma * 0.75));
}

function roleMap(
  primary: TonalPalette,
  secondary: TonalPalette,
  tertiary: TonalPalette,
  neutral: TonalPalette,
  error: TonalPalette,
  dark: boolean,
): RoleMap {
  const T = (p: TonalPalette, tone: number) => hexFromArgb(p.tone(tone));
  const surfaceTone = dark ? 6 : 98;
  const surfaceContainerTone = dark ? 12 : 95;
  const onSurfaceTone = dark ? 90 : 10;

  return {
    primary: T(primary, dark ? 80 : 40),
    onPrimary: T(primary, dark ? 20 : 100),
    primaryContainer: T(primary, dark ? 30 : 90),
    onPrimaryContainer: T(primary, dark ? 90 : 10),

    secondary: T(secondary, dark ? 80 : 40),
    onSecondary: T(secondary, dark ? 20 : 100),
    secondaryContainer: T(secondary, dark ? 30 : 90),
    onSecondaryContainer: T(secondary, dark ? 90 : 10),

    tertiary: T(tertiary, dark ? 80 : 40),
    onTertiary: T(tertiary, dark ? 20 : 100),
    tertiaryContainer: T(tertiary, dark ? 30 : 90),
    onTertiaryContainer: T(tertiary, dark ? 90 : 10),

    error: T(error, dark ? 80 : 40),
    onError: T(error, dark ? 20 : 100),
    errorContainer: T(error, dark ? 30 : 90),
    onErrorContainer: T(error, dark ? 90 : 10),

    surface: T(neutral, surfaceTone),
    onSurface: T(neutral, onSurfaceTone),
    surfaceContainer: T(neutral, surfaceContainerTone),
    outline: T(neutral, dark ? 60 : 50),
    outlineVariant: T(neutral, dark ? 30 : 80),
  };
}
