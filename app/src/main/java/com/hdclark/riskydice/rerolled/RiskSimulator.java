package com.hdclark.riskydice.rerolled;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Platform-independent Risk dice and sustained-battle simulator.
 *
 * <p>The control flow intentionally mirrors the original C++ reference implementation:
 * attackers roll up to three dice, defenders roll up to two, dice are sorted descending,
 * and ties are awarded to the defender. An attacking army count excludes the one unit that
 * must remain behind.</p>
 */
public final class RiskSimulator {
    public static final int DEFAULT_SIMULATIONS = 10_000;
    public static final int MAX_BATTLE_UNITS = 1_000;

    @FunctionalInterface
    public interface DiceSource {
        int roll();
    }

    public static final class RandomDiceSource implements DiceSource {
        private final Random random;

        public RandomDiceSource() {
            this(new Random());
        }

        public RandomDiceSource(long seed) {
            this(new Random(seed));
        }

        public RandomDiceSource(Random random) {
            this.random = Objects.requireNonNull(random, "random");
        }

        @Override
        public int roll() {
            return random.nextInt(6) + 1;
        }
    }

    public static final class RoundResult {
        public final int attackerDiceCount;
        public final int defenderDiceCount;
        public final int[] attackerRolls;
        public final int[] defenderRolls;
        public final int attackerLosses;
        public final int defenderLosses;

        private RoundResult(
                int attackerDiceCount,
                int defenderDiceCount,
                int[] attackerRolls,
                int[] defenderRolls,
                int attackerLosses,
                int defenderLosses) {
            this.attackerDiceCount = attackerDiceCount;
            this.defenderDiceCount = defenderDiceCount;
            this.attackerRolls = attackerRolls;
            this.defenderRolls = defenderRolls;
            this.attackerLosses = attackerLosses;
            this.defenderLosses = defenderLosses;
        }
    }

    public static final class RoundEstimate {
        public final int attackerDiceCount;
        public final int defenderDiceCount;
        public final int simulations;
        public final double expectedAttackerLosses;
        public final double expectedDefenderLosses;

        private RoundEstimate(
                int attackerDiceCount,
                int defenderDiceCount,
                int simulations,
                double expectedAttackerLosses,
                double expectedDefenderLosses) {
            this.attackerDiceCount = attackerDiceCount;
            this.defenderDiceCount = defenderDiceCount;
            this.simulations = simulations;
            this.expectedAttackerLosses = expectedAttackerLosses;
            this.expectedDefenderLosses = expectedDefenderLosses;
        }
    }

    public static final class BattleResult {
        public final int initialAttackers;
        public final int initialDefenders;
        public final int attackersRemaining;
        public final int defendersRemaining;
        public final int attackerLosses;
        public final int defenderLosses;
        public final boolean attackerWon;

        private BattleResult(
                int initialAttackers,
                int initialDefenders,
                int attackersRemaining,
                int defendersRemaining) {
            this.initialAttackers = initialAttackers;
            this.initialDefenders = initialDefenders;
            this.attackersRemaining = attackersRemaining;
            this.defendersRemaining = defendersRemaining;
            this.attackerLosses = initialAttackers - attackersRemaining;
            this.defenderLosses = initialDefenders - defendersRemaining;
            this.attackerWon = defendersRemaining == 0;
        }
    }

    public static final class BattleEstimate {
        public final int initialAttackers;
        public final int initialDefenders;
        public final int simulations;
        public final double attackerWinRatio;
        public final double expectedAttackerLosses;
        public final double expectedDefenderLosses;

        private BattleEstimate(
                int initialAttackers,
                int initialDefenders,
                int simulations,
                double attackerWinRatio,
                double expectedAttackerLosses,
                double expectedDefenderLosses) {
            this.initialAttackers = initialAttackers;
            this.initialDefenders = initialDefenders;
            this.simulations = simulations;
            this.attackerWinRatio = attackerWinRatio;
            this.expectedAttackerLosses = expectedAttackerLosses;
            this.expectedDefenderLosses = expectedDefenderLosses;
        }
    }

    public static final class BattleInput {
        public final int attackers;
        public final int defenders;

        private BattleInput(int attackers, int defenders) {
            this.attackers = attackers;
            this.defenders = defenders;
        }

        /**
         * Parses the same practical input language as the reference app. The attacker count is
         * the leading integer, and the defender count is the leading integer after the final v/V.
         */
        public static BattleInput parse(String raw) {
            if (raw == null) {
                throw new IllegalArgumentException("Enter a battle as XvY, for example 10v5.");
            }
            String value = raw.trim();
            int split = Math.max(value.lastIndexOf('v'), value.lastIndexOf('V'));
            if (split < 0) {
                throw new IllegalArgumentException("Enter a battle as XvY, for example 10v5.");
            }

            int attackers = parseLeadingInt(value);
            int defenders = parseLeadingInt(value.substring(split + 1));
            validateBattleUnits(attackers, defenders);
            return new BattleInput(attackers, defenders);
        }

        private static int parseLeadingInt(String text) {
            int end = 0;
            while (end < text.length() && text.charAt(end) >= '0' && text.charAt(end) <= '9') {
                end++;
            }
            if (end == 0) {
                throw new IllegalArgumentException("Both armies must contain 1 to 1000 units.");
            }
            try {
                return Integer.parseInt(text.substring(0, end));
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("Both armies must contain 1 to 1000 units.", error);
            }
        }
    }

    private final DiceSource dice;

    public RiskSimulator() {
        this(new RandomDiceSource());
    }

    public RiskSimulator(DiceSource dice) {
        this.dice = Objects.requireNonNull(dice, "dice");
    }

    public RoundResult rollRound(int attackerDiceCount, int defenderDiceCount) {
        validateRoundDice(attackerDiceCount, defenderDiceCount);
        int[] attackers = new int[attackerDiceCount];
        int[] defenders = new int[defenderDiceCount];
        for (int i = 0; i < attackers.length; i++) {
            attackers[i] = checkedRoll();
        }
        for (int i = 0; i < defenders.length; i++) {
            defenders[i] = checkedRoll();
        }
        return resolveRolls(attackers, defenders);
    }

    public static RoundResult resolveRolls(int[] attackerRolls, int[] defenderRolls) {
        Objects.requireNonNull(attackerRolls, "attackerRolls");
        Objects.requireNonNull(defenderRolls, "defenderRolls");
        validateRoundDice(attackerRolls.length, defenderRolls.length);

        int[] attackers = attackerRolls.clone();
        int[] defenders = defenderRolls.clone();
        validateDieValues(attackers);
        validateDieValues(defenders);
        sortDescending(attackers);
        sortDescending(defenders);

        int attackerLosses = 0;
        int defenderLosses = 0;
        int comparisons = Math.min(attackers.length, defenders.length);
        for (int i = 0; i < comparisons; i++) {
            if (attackers[i] > defenders[i]) {
                defenderLosses++;
            } else {
                attackerLosses++;
            }
        }

        return new RoundResult(
                attackers.length,
                defenders.length,
                attackers,
                defenders,
                attackerLosses,
                defenderLosses);
    }

    public RoundEstimate estimateRoundLosses(int attackerDiceCount, int defenderDiceCount) {
        return estimateRoundLosses(attackerDiceCount, defenderDiceCount, DEFAULT_SIMULATIONS);
    }

    public RoundEstimate estimateRoundLosses(
            int attackerDiceCount,
            int defenderDiceCount,
            int simulations) {
        validateRoundDice(attackerDiceCount, defenderDiceCount);
        validateSimulationCount(simulations);

        long attackerLosses = 0;
        long defenderLosses = 0;
        for (int i = 0; i < simulations; i++) {
            int code = rollLossCode(attackerDiceCount, defenderDiceCount);
            attackerLosses += decodeAttackerLosses(code);
            defenderLosses += decodeDefenderLosses(code);
        }

        return new RoundEstimate(
                attackerDiceCount,
                defenderDiceCount,
                simulations,
                (double) attackerLosses / simulations,
                (double) defenderLosses / simulations);
    }

    public BattleResult simulateBattle(int attackers, int defenders) {
        validateBattleUnits(attackers, defenders);
        return simulateBattleUnchecked(attackers, defenders);
    }

    public BattleEstimate estimateBattleLosses(int attackers, int defenders) {
        return estimateBattleLosses(attackers, defenders, DEFAULT_SIMULATIONS);
    }

    public BattleEstimate estimateBattleLosses(int attackers, int defenders, int simulations) {
        validateBattleUnits(attackers, defenders);
        validateSimulationCount(simulations);

        long attackerWins = 0;
        long attackerLosses = 0;
        long defenderLosses = 0;

        for (int i = 0; i < simulations; i++) {
            BattleResult result = simulateBattleUnchecked(attackers, defenders);
            if (result.attackerWon) {
                attackerWins++;
            }
            attackerLosses += result.attackerLosses;
            defenderLosses += result.defenderLosses;
        }

        return new BattleEstimate(
                attackers,
                defenders,
                simulations,
                (double) attackerWins / simulations,
                (double) attackerLosses / simulations,
                (double) defenderLosses / simulations);
    }

    private BattleResult simulateBattleUnchecked(int initialAttackers, int initialDefenders) {
        int attackers = initialAttackers;
        int defenders = initialDefenders;

        while (attackers > 0 && defenders > 0) {
            int battlingAttackers = Math.min(attackers, 3);
            int battlingDefenders = Math.min(defenders, 2);

            // Match the reference implementation: remove participating units, simulate survivors,
            // and then restore the survivors to the armies.
            attackers -= battlingAttackers;
            defenders -= battlingDefenders;

            int lossCode = rollLossCode(battlingAttackers, battlingDefenders);
            attackers += battlingAttackers - decodeAttackerLosses(lossCode);
            defenders += battlingDefenders - decodeDefenderLosses(lossCode);
        }

        return new BattleResult(initialAttackers, initialDefenders, attackers, defenders);
    }

    /** Encodes attacker losses in the high byte and defender losses in the low byte. */
    private int rollLossCode(int attackerDiceCount, int defenderDiceCount) {
        int a0 = checkedRoll();
        int a1 = attackerDiceCount >= 2 ? checkedRoll() : 0;
        int a2 = attackerDiceCount >= 3 ? checkedRoll() : 0;
        int d0 = checkedRoll();
        int d1 = defenderDiceCount >= 2 ? checkedRoll() : 0;

        if (a1 > a0) {
            int swap = a0;
            a0 = a1;
            a1 = swap;
        }
        if (a2 > a1) {
            int swap = a1;
            a1 = a2;
            a2 = swap;
        }
        if (a1 > a0) {
            int swap = a0;
            a0 = a1;
            a1 = swap;
        }
        if (d1 > d0) {
            int swap = d0;
            d0 = d1;
            d1 = swap;
        }

        int attackerLosses = 0;
        int defenderLosses = 0;
        if (a0 > d0) {
            defenderLosses++;
        } else {
            attackerLosses++;
        }
        if (attackerDiceCount >= 2 && defenderDiceCount >= 2) {
            if (a1 > d1) {
                defenderLosses++;
            } else {
                attackerLosses++;
            }
        }
        return (attackerLosses << 8) | defenderLosses;
    }

    private int checkedRoll() {
        int value = dice.roll();
        if (value < 1 || value > 6) {
            throw new IllegalStateException("Dice source returned " + value + "; expected 1 through 6.");
        }
        return value;
    }

    private static int decodeAttackerLosses(int code) {
        return (code >>> 8) & 0xFF;
    }

    private static int decodeDefenderLosses(int code) {
        return code & 0xFF;
    }

    private static void validateRoundDice(int attackers, int defenders) {
        if (attackers < 1 || attackers > 3 || defenders < 1 || defenders > 2) {
            throw new IllegalArgumentException(
                    "Attack simulation requires 1 to 3 attacker dice and 1 to 2 defender dice.");
        }
    }

    private static void validateBattleUnits(int attackers, int defenders) {
        if (attackers < 1 || attackers > MAX_BATTLE_UNITS
                || defenders < 1 || defenders > MAX_BATTLE_UNITS) {
            throw new IllegalArgumentException(String.format(
                    Locale.US,
                    "Both armies must contain 1 to %d units.",
                    MAX_BATTLE_UNITS));
        }
    }

    private static void validateSimulationCount(int simulations) {
        if (simulations < 1) {
            throw new IllegalArgumentException("Simulation count must be positive.");
        }
    }

    private static void validateDieValues(int[] rolls) {
        for (int roll : rolls) {
            if (roll < 1 || roll > 6) {
                throw new IllegalArgumentException("Die values must be between 1 and 6.");
            }
        }
    }

    private static void sortDescending(int[] values) {
        Arrays.sort(values);
        for (int left = 0, right = values.length - 1; left < right; left++, right--) {
            int swap = values[left];
            values[left] = values[right];
            values[right] = swap;
        }
    }
}
