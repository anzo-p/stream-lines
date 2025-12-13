const tseslint = require('@typescript-eslint/eslint-plugin');
const tsparser = require('@typescript-eslint/parser');

module.exports = [
  {
    ignores: [
      '.DS_Store',
      'cdk.out/**',
      'dist/**',
      'node_modules/**',
      '.env*',
      'readme.md',
      'package-lock.json',
      'pnpm-lock.yaml',
      'yarn.lock'
    ]
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tsparser
    },
    plugins: {
      '@typescript-eslint': tseslint
    },
    rules: {
      ...tseslint.configs.recommended.rules
    }
  }
];
