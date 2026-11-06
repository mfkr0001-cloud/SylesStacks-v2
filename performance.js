// Настройки здесь являются предпочтениями интерфейса, не меняют системный FPS или питание.
export const supportedProfiles = ['saving', 'balanced', 'performance'];
export function isSupportedProfile(value) { return supportedProfiles.includes(value); }
