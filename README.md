# Risky Dice: Re-Rolled

An Android port of [RiskyDice](https://github.com/hdclark/RiskyDice), the small Linux/SFML dice simulator for classic **Risk** battles.

The simulation and sampling rules intentionally follow the reference implementation:

- Attackers roll 1–3 dice; defenders roll 1–2 dice.
- Both sides sort their dice from highest to lowest and compare them in order.
- The defender wins ties.
- Sustained battles continue until one participating army reaches zero.
- An entered attacker count excludes the one unit that Risk requires be left behind.
- Round and battle forecasts each use 10,000 Monte Carlo samples, matching the original app.
- Battle inputs are limited to 1–1000 units per side.

## Android controls

| Reference input | Android control | Result |
| --- | --- | --- |
| Space | **Roll all matchups** | Samples 3v2, 3v1, 2v2, 2v1, 1v2, and 1v1 rounds. |
| `s` | **Estimate round losses** | Shows 10,000-sample expected losses for all six matchups. |
| `XvY` | Battle input field | Enters attacking and defending participating units, such as `10v5`. |
| Enter / Return | **Simulate battle** | Shows one sustained-battle sample and a 10,000-sample forecast. |
| `c` | **Clear** | Clears input and restores help. |
| `q` / Escape | Back / system close | Exits the app. |

Hardware keyboard shortcuts from the original app remain supported.

## Build APKs through GitHub Actions

This repository intentionally treats GitHub Actions as the supported build environment. No local Gradle wrapper is committed.

1. Open **Actions** in GitHub.
2. Select **Build APKs**.
3. Choose **Run workflow**.
4. Download the `RiskyDice-ReRolled-APKs-*` artifact after the job succeeds.

Every run executes unit tests and Android lint, then builds:

- `app-debug.apk`, installable for debugging.
- `app-release-unsigned.apk` when release signing secrets are absent.
- A signed release APK when all signing secrets below are configured.

### Optional release signing secrets

Configure these Actions secrets in the repository:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded JKS/keystore file.
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The keystore is decoded only inside the ephemeral GitHub-hosted runner.

## Verification

The core rules are platform-independent Java and have deterministic dice-source injection for testing. The test suite covers:

- Defender-favoured ties and descending dice pairing.
- Input parsing and 1–1000 army limits.
- Battle termination and unit accounting.
- Seeded Monte Carlo expectations.
- Exhaustive enumeration of all 7,776 possible 3v2 rolls, verifying the classic outcome counts:
  - attacker loses two: 2,275;
  - each side loses one: 2,611;
  - defender loses two: 2,890.

For an SDK-free smoke test of the core logic, CI-compatible maintainers can also compile `tools/CoreSelfTest.java` with the two platform-independent source files using any JDK 17 installation.

## Project structure

- `RiskSimulator.java`: reference-faithful simulation and sampling engine.
- `RiskFormatter.java`: output text matching the information shown by the SFML app.
- `MainActivity.java`: Android touch and hardware-key interaction.
- `RiskSimulatorTest.java`: exhaustive and deterministic correctness tests.
- `.github/workflows/build-apk.yml`: the supported debug/release build pipeline.

## License

GPL-3.0, matching the original RiskyDice repository. This is an unofficial utility for the board game Risk and is not affiliated with its publisher.
