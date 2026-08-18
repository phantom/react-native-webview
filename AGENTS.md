# Repository Instructions

## Purpose

This repository is Phantom's fork of `react-native-webview`, a community-maintained WebView component for React Native. It supports iOS, Android, Windows, macOS, and both React Native architectures.

## Layout

- `src/`: TypeScript and React Native component source.
- `android/`, `apple/`, `ios/`, `macos/`, `windows/`: platform-specific native implementations.
- `example/`: example and native test applications.
- `__tests__/` and `jest-setups/`: Jest tests and setup.
- `docs/`: usage, API, and contribution documentation.

## Commands

Install dependencies with `yarn install` (CI uses `yarn --frozen-lockfile`).

- `yarn ci`: run the TypeScript and ESLint checks used by the repository's CI script.
- `yarn lint`: run TypeScript checking and ESLint directly.
- `yarn build`: compile `src/` into `lib/` with Babel.
- `yarn android`, `yarn ios`, `yarn macos`, `yarn windows`: build and run the corresponding example application; follow `docs/Contributing.md` for platform prerequisites.
- `yarn test:windows`: run the configured Windows Jest suite.

## Contribution Constraints

- Use TypeScript for JavaScript-layer source changes.
- Test platform changes through the relevant example application as described in `docs/Contributing.md`.
- Follow `VERSIONING.md` and add a Changesets entry for publishable package changes.
