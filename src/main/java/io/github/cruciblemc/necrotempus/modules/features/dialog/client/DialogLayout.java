package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import java.util.List;

final class DialogLayout {

    private static final int BUTTON_GAP = 2;

    private DialogLayout() {}

    static int panelWidth(int screenWidth, int requiredContentWidth) {
        return Math.min(Math.max(1, screenWidth - 32), Math.max(200, requiredContentWidth) + 24);
    }

    static int labeledControlX(int panelLeft, int panelWidth, int labelWidth, int gap, int controlWidth) {
        return panelLeft + (panelWidth - labelWidth - gap - controlWidth) / 2 + labelWidth + gap;
    }

    static int buttonX(int panelLeft, int panelWidth, List<Integer> widths, int columns, int index) {
        return buttonX(panelLeft, panelWidth, widths, columns, index, BUTTON_GAP);
    }

    static int buttonX(int panelLeft, int panelWidth, List<Integer> widths, int columns, int index, int gap) {
        if (columns < 1 || index < 0 || index >= widths.size()) throw new IllegalArgumentException("Invalid grid");
        int row = index / columns;
        int rowStart = row * columns;
        int rowEnd = Math.min(widths.size(), rowStart + columns);
        if (rowEnd - rowStart < columns) {
            int rowWidth = 0;
            for (int i = rowStart; i < rowEnd; i++) rowWidth += widths.get(i);
            rowWidth += Math.max(0, rowEnd - rowStart - 1) * gap;
            int x = panelLeft + (panelWidth - rowWidth) / 2;
            for (int i = rowStart; i < index; i++) x += widths.get(i) + gap;
            return x;
        }
        int[] columnWidths = new int[columns];
        for (int i = 0; i < widths.size(); i++) {
            int column = i % columns;
            columnWidths[column] = Math.max(columnWidths[column], widths.get(i));
        }
        int rowWidth = 0;
        for (int columnWidth : columnWidths) rowWidth += columnWidth;
        rowWidth += Math.max(0, columns - 1) * gap;
        int x = panelLeft + (panelWidth - rowWidth) / 2;
        for (int column = 0; column < index % columns; column++) x += columnWidths[column] + gap;
        return x;
    }

    static int gridWidth(List<Integer> widths, int columns, int gap) {
        if (columns < 1) throw new IllegalArgumentException("Invalid grid");
        int[] columnWidths = new int[columns];
        for (int i = 0; i < widths.size(); i++) {
            int column = i % columns;
            columnWidths[column] = Math.max(columnWidths[column], widths.get(i));
        }
        int width = 0;
        for (int columnWidth : columnWidths) width += columnWidth;
        return width + Math.max(0, Math.min(columns, widths.size()) - 1) * gap;
    }
}
