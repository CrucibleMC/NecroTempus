package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

final class DialogTextArea {

    private final DialogTextBuffer buffer;
    private int x;
    private int y;
    private int width;
    private int height;
    private int firstVisibleLine;
    private int horizontalScroll;
    private int cursorTicks;
    private boolean focused;
    private boolean dragging;

    DialogTextArea(DialogTextBuffer buffer) {
        this.buffer = buffer;
    }

    DialogTextBuffer getBuffer() {
        return buffer;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    int getFirstVisibleLine() {
        return firstVisibleLine;
    }

    int getHorizontalScroll() {
        return horizontalScroll;
    }

    int getVisibleLineCount() {
        return Math.max(1, (height - 6) / 9);
    }

    int getLineCount() {
        return lineCount();
    }

    boolean isFocused() {
        return focused;
    }

    void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) dragging = false;
    }

    void setBounds(int x, int y, int width, int height, FontRenderer font) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        ensureCursorVisible(font);
    }

    boolean mouseClicked(int mouseX, int mouseY, int mouseButton, FontRenderer font) {
        if (!contains(mouseX, mouseY) || mouseButton != 0) {
            setFocused(false);
            return false;
        }
        focused = true;
        dragging = true;
        buffer.setCursorPosition(cursorAt(mouseX, mouseY, font), GuiScreen.isShiftKeyDown());
        ensureCursorVisible(font);
        return true;
    }

    void mouseDragged(int mouseX, int mouseY, FontRenderer font) {
        if (!dragging) return;
        if (mouseY < y + 3) firstVisibleLine = Math.max(0, firstVisibleLine - 1);
        if (mouseY >= y + height - 3) firstVisibleLine = Math.min(lineCount() - 1, firstVisibleLine + 1);
        buffer.setCursorPosition(cursorAt(mouseX, mouseY, font), true);
        ensureCursorVisible(font);
    }

    void mouseReleased() {
        dragging = false;
    }

    boolean keyTyped(char character, int keyCode, FontRenderer font) {
        if (!focused) return false;
        boolean control = GuiScreen.isCtrlKeyDown();
        boolean shift = GuiScreen.isShiftKeyDown();
        if (control) {
            if (keyCode == Keyboard.KEY_A) {
                buffer.selectAll();
                return true;
            }
            if (keyCode == Keyboard.KEY_C || keyCode == Keyboard.KEY_X) {
                if (buffer.hasSelection()) GuiScreen.setClipboardString(buffer.getSelectedText());
                if (keyCode == Keyboard.KEY_X) buffer.deleteSelection();
                return true;
            }
            if (keyCode == Keyboard.KEY_V) {
                buffer.insert(GuiScreen.getClipboardString());
                ensureCursorVisible(font);
                return true;
            }
        }
        if (keyCode == Keyboard.KEY_LEFT) buffer.moveHorizontal(-1, shift, control);
        else if (keyCode == Keyboard.KEY_RIGHT) buffer.moveHorizontal(1, shift, control);
        else if (keyCode == Keyboard.KEY_UP) buffer.moveVertical(-1, shift);
        else if (keyCode == Keyboard.KEY_DOWN) buffer.moveVertical(1, shift);
        else if (keyCode == Keyboard.KEY_HOME) buffer.moveToLineBoundary(false, control, shift);
        else if (keyCode == Keyboard.KEY_END) buffer.moveToLineBoundary(true, control, shift);
        else if (keyCode == Keyboard.KEY_BACK) buffer.deleteBackward();
        else if (keyCode == Keyboard.KEY_DELETE) buffer.deleteForward();
        else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) buffer.insert("\n");
        else if (character >= 32 && !Character.isISOControl(character)) buffer.insert(String.valueOf(character));
        else return false;
        ensureCursorVisible(font);
        cursorTicks = 0;
        return true;
    }

    void updateCursorCounter() {
        cursorTicks++;
    }

    boolean shouldDrawCursor() {
        return focused && cursorTicks / 6 % 2 == 0;
    }

    int lineStart(int line) {
        String text = buffer.getText();
        int start = 0;
        for (int i = 0; i < line; i++) {
            int newline = text.indexOf('\n', start);
            if (newline < 0) return text.length();
            start = newline + 1;
        }
        return start;
    }

    int lineEnd(int line) {
        String text = buffer.getText();
        int start = lineStart(line);
        int newline = text.indexOf('\n', start);
        return newline < 0 ? text.length() : newline;
    }

    int cursorLine() {
        String text = buffer.getText();
        int line = 0;
        for (int i = 0; i < buffer.getCursorPosition(); i++) if (text.charAt(i) == '\n') line++;
        return line;
    }

    private boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private int cursorAt(int mouseX, int mouseY, FontRenderer font) {
        int visibleLine = Math.max(0, Math.min(getVisibleLineCount() - 1, (mouseY - y - 3) / 9));
        int line = Math.min(lineCount() - 1, firstVisibleLine + visibleLine);
        int start = lineStart(line);
        int end = lineEnd(line);
        int targetWidth = Math.max(0, mouseX - x - 4 + horizontalScroll);
        String text = buffer.getText();
        int position = start;
        while (position < end) {
            int next = text.offsetByCodePoints(position, 1);
            int before = font.getStringWidth(text.substring(start, position));
            int after = font.getStringWidth(text.substring(start, next));
            if (targetWidth < (before + after) / 2) return position;
            position = next;
        }
        return end;
    }

    private void ensureCursorVisible(FontRenderer font) {
        int lineCount = lineCount();
        int visibleLines = getVisibleLineCount();
        int cursorLine = cursorLine();
        firstVisibleLine = Math.max(0, Math.min(lineCount - visibleLines, firstVisibleLine));
        if (cursorLine < firstVisibleLine) firstVisibleLine = cursorLine;
        if (cursorLine >= firstVisibleLine + visibleLines) firstVisibleLine = cursorLine - visibleLines + 1;

        int start = lineStart(cursorLine);
        int cursor = buffer.getCursorPosition();
        int cursorWidth = font.getStringWidth(
            buffer.getText()
                .substring(start, cursor));
        int availableWidth = Math.max(1, width - 8);
        if (cursorWidth < horizontalScroll) horizontalScroll = cursorWidth;
        if (cursorWidth > horizontalScroll + availableWidth) horizontalScroll = cursorWidth - availableWidth;
    }

    private int lineCount() {
        String text = buffer.getText();
        int count = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') count++;
        return count;
    }
}
