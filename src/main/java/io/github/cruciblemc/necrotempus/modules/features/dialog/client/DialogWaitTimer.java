package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

final class DialogWaitTimer {

    private int ticks;

    int tick() {
        return ticks++;
    }

    static boolean isReturnVisible(int elapsedTicks) {
        return elapsedTicks >= 20;
    }

    static boolean isReturnEnabled(int elapsedTicks) {
        return elapsedTicks >= 100;
    }

    static int countdownStep(int elapsedTicks) {
        return Math.min(elapsedTicks / 20, 5);
    }
}
