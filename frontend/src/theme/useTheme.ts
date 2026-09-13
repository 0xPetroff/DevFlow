import { use } from 'react';

import { ThemeContext, type ThemeContextValue } from '@/theme/ThemeContext';

export function useTheme(): ThemeContextValue {
  const context = use(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used inside a ThemeProvider');
  }
  return context;
}
