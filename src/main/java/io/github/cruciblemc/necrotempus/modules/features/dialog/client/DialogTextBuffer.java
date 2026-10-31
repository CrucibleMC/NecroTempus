package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

final class DialogTextBuffer {

    private final int maxLength;
    private final int maxLines;
    private String text;
    private int cursor;
    private int selectionAnchor;
    private int preferredColumn = -1;

    DialogTextBuffer(String initial, int maxLength, int maxLines) {
        if (initial == null || maxLength < 1 || maxLines < 1 || maxLines > 64) {
            throw new IllegalArgumentException("Invalid text limits");
        }
        this.maxLength = maxLength;
        this.maxLines = maxLines;
        text = normalize(initial);
        if (!fits(text)) throw new IllegalArgumentException("Initial text exceeds its limits");
        cursor = text.length();
        selectionAnchor = cursor;
    }

    String getText() {
        return text;
    }

    int getCursorPosition() {
        return cursor;
    }

    void setCursorPosition(int position, boolean extendSelection) {
        cursor = clamp(position);
        preferredColumn = -1;
        if (!extendSelection) selectionAnchor = cursor;
    }

    int getSelectionStart() {
        return Math.min(cursor, selectionAnchor);
    }

    int getSelectionEnd() {
        return Math.max(cursor, selectionAnchor);
    }

    void selectAll() {
        selectionAnchor = 0;
        cursor = text.length();
        preferredColumn = -1;
    }

    String getSelectedText() {
        return text.substring(getSelectionStart(), getSelectionEnd());
    }

    boolean hasSelection() {
        return cursor != selectionAnchor;
    }

    void deleteSelection() {
        if (!hasSelection()) return;
        int start = getSelectionStart();
        text = text.substring(0, start) + text.substring(getSelectionEnd());
        cursor = start;
        selectionAnchor = start;
        preferredColumn = -1;
    }

    boolean insert(String value) {
        if (value == null) return false;
        String addition = normalize(value);
        int start = getSelectionStart();
        int end = getSelectionEnd();
        String candidate = text.substring(0, start) + addition + text.substring(end);
        if (!fits(candidate)) return false;
        text = candidate;
        cursor = start + addition.length();
        selectionAnchor = cursor;
        preferredColumn = -1;
        return true;
    }

    void moveHorizontal(int direction, boolean extendSelection, boolean byWord) {
        if (direction == 0) return;
        if (!extendSelection && hasSelection()) {
            cursor = direction < 0 ? getSelectionStart() : getSelectionEnd();
            selectionAnchor = cursor;
            preferredColumn = -1;
            return;
        }
        int position = cursor;
        if (byWord) {
            if (direction < 0) {
                while (position > 0 && Character.isWhitespace(text.codePointBefore(position))) {
                    position = text.offsetByCodePoints(position, -1);
                }
                while (position > 0 && !Character.isWhitespace(text.codePointBefore(position))) {
                    position = text.offsetByCodePoints(position, -1);
                }
            } else {
                while (position < text.length() && !Character.isWhitespace(text.codePointAt(position))) {
                    position = text.offsetByCodePoints(position, 1);
                }
                while (position < text.length() && Character.isWhitespace(text.codePointAt(position))) {
                    position = text.offsetByCodePoints(position, 1);
                }
            }
        } else if (direction < 0 && position > 0) {
            position = text.offsetByCodePoints(position, -1);
        } else if (direction > 0 && position < text.length()) {
            position = text.offsetByCodePoints(position, 1);
        }
        setCursorPosition(position, extendSelection);
    }

    void moveVertical(int direction, boolean extendSelection) {
        if (direction == 0) return;
        int currentStart = lineStartAt(cursor);
        int currentEnd = lineEndAt(cursor);
        if (preferredColumn < 0) preferredColumn = text.codePointCount(currentStart, cursor);
        int targetStart;
        int targetEnd;
        if (direction < 0) {
            if (currentStart == 0) return;
            targetEnd = currentStart - 1;
            targetStart = lineStartAt(targetEnd);
        } else {
            if (currentEnd == text.length()) return;
            targetStart = currentEnd + 1;
            int nextBreak = text.indexOf('\n', targetStart);
            targetEnd = nextBreak < 0 ? text.length() : nextBreak;
        }
        int targetColumn = Math.min(preferredColumn, text.codePointCount(targetStart, targetEnd));
        cursor = text.offsetByCodePoints(targetStart, targetColumn);
        if (!extendSelection) selectionAnchor = cursor;
    }

    void moveToLineBoundary(boolean end, boolean document, boolean extendSelection) {
        int position = document ? (end ? text.length() : 0) : (end ? lineEndAt(cursor) : lineStartAt(cursor));
        setCursorPosition(position, extendSelection);
    }

    void deleteBackward() {
        if (hasSelection()) {
            deleteSelection();
        } else if (cursor > 0) {
            int previous = text.offsetByCodePoints(cursor, -1);
            text = text.substring(0, previous) + text.substring(cursor);
            cursor = previous;
            selectionAnchor = cursor;
            preferredColumn = -1;
        }
    }

    void deleteForward() {
        if (hasSelection()) {
            deleteSelection();
        } else if (cursor < text.length()) {
            int next = text.offsetByCodePoints(cursor, 1);
            text = text.substring(0, cursor) + text.substring(next);
            selectionAnchor = cursor;
            preferredColumn = -1;
        }
    }

    private int clamp(int position) {
        int result = Math.max(0, Math.min(text.length(), position));
        if (result > 0 && result < text.length()
            && Character.isLowSurrogate(text.charAt(result))
            && Character.isHighSurrogate(text.charAt(result - 1))) {
            return result - 1;
        }
        return result;
    }

    private int lineStartAt(int position) {
        return text.lastIndexOf('\n', position - 1) + 1;
    }

    private int lineEndAt(int position) {
        int end = text.indexOf('\n', position);
        return end < 0 ? text.length() : end;
    }

    private boolean fits(String value) {
        return value.length() <= maxLength && lineCount(value) <= maxLines;
    }

    private static int lineCount(String value) {
        int lines = 1;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == '\n') lines++;
        return lines;
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n")
            .replace('\r', '\n');
    }
}
