package com.hdclark.riskydice.rerolled;

import java.util.Locale;

public final class RiskFormatter {
    private RiskFormatter() {
    }

    public static String usage() {
        return "Press or tap:\n"
                + "-------------------------------------------------\n"
                + "Space / Roll all matchups: simulate single attack dice rolling.\n"
                + "S / Estimate round losses: sample single attack losses.\n"
                + "XvY (for example 10v5): enter a sustained battle.\n"
                + "Enter / Simulate battle: launch battle simulation.\n"
                + "C / Clear: clear battle input and results.\n"
                + "Q, ESC, or Back: exit the app.";
    }

    public static String roundResult(RiskSimulator.RoundResult result) {
        StringBuilder output = new StringBuilder();
        appendHeader(output, result.attackerDiceCount, result.defenderDiceCount);
        output.append("Attacker: ");
        appendRolls(output, result.attackerRolls);
        output.append('\n');
        output.append("Defender: ");
        appendRolls(output, result.defenderRolls);
        output.append("\n\n");
        output.append("Attacker loses: ").append(result.attackerLosses).append('\n');
        output.append("Defender loses: ").append(result.defenderLosses);
        return output.toString();
    }

    public static String roundEstimate(RiskSimulator.RoundEstimate estimate) {
        StringBuilder output = new StringBuilder();
        appendHeader(output, estimate.attackerDiceCount, estimate.defenderDiceCount);
        output.append("Expected losses\n");
        output.append("  (").append(estimate.simulations).append(" sims)\n\n");
        output.append(String.format(
                Locale.US,
                "Attacker loses: %.2f\nDefender loses: %.2f",
                estimate.expectedAttackerLosses,
                estimate.expectedDefenderLosses));
        return output.toString();
    }

    public static String battleResult(RiskSimulator.BattleResult result) {
        StringBuilder output = new StringBuilder();
        appendHeader(output, result.initialAttackers, result.initialDefenders);
        output.append("Attacker loses: ").append(result.attackerLosses).append('\n');
        output.append("Defender loses: ").append(result.defenderLosses).append("\n\n");
        output.append(result.attackerWon ? "Attacker" : "Defender").append(" wins.\n");
        output.append(result.attackerWon ? result.attackersRemaining : result.defendersRemaining)
                .append(" units remain.");
        return output.toString();
    }

    public static String battleEstimate(RiskSimulator.BattleEstimate estimate) {
        StringBuilder output = new StringBuilder();
        appendHeader(output, estimate.initialAttackers, estimate.initialDefenders);
        output.append("Expected losses\n");
        output.append("  (").append(estimate.simulations).append(" sims)\n\n");
        output.append(String.format(
                Locale.US,
                "Attacker win: %.1f%%\nAttacker loses: %.2f\nDefender loses: %.2f",
                estimate.attackerWinRatio * 100.0,
                estimate.expectedAttackerLosses,
                estimate.expectedDefenderLosses));
        return output.toString();
    }

    private static void appendHeader(StringBuilder output, int attackers, int defenders) {
        output.append(attackers).append(" attacking ").append(defenders).append('\n');
        output.append("-----------------\n");
    }

    private static void appendRolls(StringBuilder output, int[] rolls) {
        for (int roll : rolls) {
            output.append(roll).append(' ');
        }
    }
}
