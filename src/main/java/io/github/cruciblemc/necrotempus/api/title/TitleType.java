package io.github.cruciblemc.necrotempus.api.title;

public enum TitleType {

    TITLE, SUBTITLE;

    public static TitleType of(String name) {

        for (TitleType titleType : values()) {
            if (titleType.name().equalsIgnoreCase(name))
                return titleType;
        }

        return TITLE;
    }

}
