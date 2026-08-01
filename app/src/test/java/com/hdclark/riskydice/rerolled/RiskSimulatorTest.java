package com.hdclark.riskydice.rerolled;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;

public final class RiskSimulatorTest {
    @Test
    public void tiesBelongToDefenderAndRollsAreSorted() {
        RiskSimulator.RoundResult result = RiskSimulator.resolveRolls(
                new int[] {1, 6, 4},
                new int[] {3, 6});

        assertArrayEquals(new int[] {6, 4, 1}, result.attackerRolls);
        assertArrayEquals(new int[] {6, 3}, result.defenderRolls);
        assertEquals(1, result.attackerLosses);
        assertEquals(1, result.defenderLosses);
    }

    @Test
    public void oneVersusOneHasExactClassicRiskOutcomeCounts() {
        int attackerWins = 0;
        int attackerLoses = 0;
        for (int attacker = 1; attacker <= 6; attacker++) {
            for (int defender = 1; defender <= 6; defender++) {
                RiskSimulator.RoundResult result = RiskSimulator.resolveRolls(
                        new int[] {attacker}, new int[] {defender});
                attackerWins += result.defenderLosses;
                attackerLoses += result.attackerLosses;
            }
        }
        assertEquals(15, attackerWins);
        assertEquals(21, attackerLoses);
    }

    @Test
    public void threeVersusTwoHasExactClassicRiskOutcomeCounts() {
        int attackerLosesTwo = 0;
        int bothLoseOne = 0;
        int defenderLosesTwo = 0;
        for (int a = 1; a <= 6; a++) {
            for (int b = 1; b <= 6; b++) {
                for (int c = 1; c <= 6; c++) {
                    for (int d = 1; d <= 6; d++) {
                        for (int e = 1; e <= 6; e++) {
                            RiskSimulator.RoundResult result = RiskSimulator.resolveRolls(
                                    new int[] {a, b, c}, new int[] {d, e});
                            if (result.attackerLosses == 2) {
                                attackerLosesTwo++;
                            } else if (result.defenderLosses == 2) {
                                defenderLosesTwo++;
                            } else {
                                bothLoseOne++;
                            }
                        }
                    }
                }
            }
        }
        assertEquals(2275, attackerLosesTwo);
        assertEquals(2611, bothLoseOne);
        assertEquals(2890, defenderLosesTwo);
        assertEquals(7776, attackerLosesTwo + bothLoseOne + defenderLosesTwo);
    }

    @Test
    public void deterministicBattlePreservesUnitsAndFindsWinner() {
        RiskSimulator simulator = new RiskSimulator(new QueueDice(6, 6, 6, 1, 1));
        RiskSimulator.BattleResult result = simulator.simulateBattle(3, 2);

        assertTrue(result.attackerWon);
        assertEquals(3, result.attackersRemaining);
        assertEquals(0, result.defendersRemaining);
        assertEquals(0, result.attackerLosses);
        assertEquals(2, result.defenderLosses);
    }

    @Test
    public void deterministicTieBattleGoesToDefender() {
        RiskSimulator simulator = new RiskSimulator(new QueueDice(1, 6, 6));
        RiskSimulator.BattleResult result = simulator.simulateBattle(1, 2);

        assertFalse(result.attackerWon);
        assertEquals(0, result.attackersRemaining);
        assertEquals(2, result.defendersRemaining);
    }

    @Test
    public void parserUsesLastVLikeReferenceImplementation() {
        RiskSimulator.BattleInput parsed = RiskSimulator.BattleInput.parse("10v5V3");
        assertEquals(10, parsed.attackers);
        assertEquals(3, parsed.defenders);
    }

    @Test
    public void parserAcceptsUppercaseAndLimitsArmies() {
        RiskSimulator.BattleInput parsed = RiskSimulator.BattleInput.parse("1000V1");
        assertEquals(1000, parsed.attackers);
        assertEquals(1, parsed.defenders);
        assertThrows(IllegalArgumentException.class,
                () -> RiskSimulator.BattleInput.parse("1001v1"));
        assertThrows(IllegalArgumentException.class,
                () -> RiskSimulator.BattleInput.parse("10"));
    }

    @Test
    public void seededMonteCarloStaysNearExhaustiveThreeVersusTwoExpectation() {
        RiskSimulator simulator = new RiskSimulator(new RiskSimulator.RandomDiceSource(0x5EEDL));
        RiskSimulator.RoundEstimate estimate = simulator.estimateRoundLosses(3, 2, 100_000);
        assertEquals(7161.0 / 7776.0, estimate.expectedAttackerLosses, 0.015);
        assertEquals(8391.0 / 7776.0, estimate.expectedDefenderLosses, 0.015);
    }

    private static final class QueueDice implements RiskSimulator.DiceSource {
        private final Queue<Integer> values = new ArrayDeque<>();

        private QueueDice(int... rolls) {
            for (int roll : rolls) {
                values.add(roll);
            }
        }

        @Override
        public int roll() {
            if (values.isEmpty()) {
                throw new AssertionError("Test dice queue exhausted");
            }
            return values.remove();
        }
    }
}
