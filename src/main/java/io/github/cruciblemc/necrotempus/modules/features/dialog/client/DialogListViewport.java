package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

final class DialogListViewport {

    private DialogListViewport() {}

    static int clampPixelOffset(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(Math.max(0, contentHeight - viewportHeight), offset));
    }

    static boolean contains(int y, int top, int height) {
        return y >= top && y < top + height;
    }

    static boolean fullyVisible(int y, int elementHeight, int top, int viewportHeight) {
        return y >= top && y + elementHeight <= top + viewportHeight;
    }
}
