import com.hdclark.riskydice.rerolled.RiskSimulator;

public final class CoreSelfTest {
    public static void main(String[] args) {
        testExactThreeVersusTwo();
        testBattleAccounting();
        testParser();
        System.out.println("CoreSelfTest: all checks passed");
    }

    private static void testExactThreeVersusTwo() {
        int loseTwo = 0;
        int split = 0;
        int winTwo = 0;
        for (int a = 1; a <= 6; a++)
            for (int b = 1; b <= 6; b++)
                for (int c = 1; c <= 6; c++)
                    for (int d = 1; d <= 6; d++)
                        for (int e = 1; e <= 6; e++) {
                            RiskSimulator.RoundResult r = RiskSimulator.resolveRolls(
                                    new int[] {a, b, c}, new int[] {d, e});
                            if (r.attackerLosses == 2) loseTwo++;
                            else if (r.defenderLosses == 2) winTwo++;
                            else split++;
                        }
        require(loseTwo == 2275, "3v2 attacker-loses-two count");
        require(split == 2611, "3v2 split count");
        require(winTwo == 2890, "3v2 defender-loses-two count");
    }

    private static void testBattleAccounting() {
        RiskSimulator simulator = new RiskSimulator(new RiskSimulator.RandomDiceSource(123456789L));
        for (int attackers = 1; attackers <= 40; attackers++) {
            for (int defenders = 1; defenders <= 40; defenders++) {
                RiskSimulator.BattleResult r = simulator.simulateBattle(attackers, defenders);
                require(r.attackersRemaining + r.attackerLosses == attackers, "attacker accounting");
                require(r.defendersRemaining + r.defenderLosses == defenders, "defender accounting");
                require((r.attackersRemaining == 0) != (r.defendersRemaining == 0), "one winner");
            }
        }
    }

    private static void testParser() {
        RiskSimulator.BattleInput normal = RiskSimulator.BattleInput.parse("10v5");
        require(normal.attackers == 10 && normal.defenders == 5, "normal parser");
        RiskSimulator.BattleInput legacy = RiskSimulator.BattleInput.parse("10v5V3");
        require(legacy.attackers == 10 && legacy.defenders == 3, "last-v parser");
        try {
            RiskSimulator.BattleInput.parse("0v5");
            throw new AssertionError("range parser");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
