package io.github.cruciblemc.necrotempus.api.bossbar;

public enum BossBarType {

    FLAT("flat"),
    NOTCHED_6("notched_6"),
    NOTCHED_10("notched_10"),
    NOTCHED_12("notched_12"),
    NOTCHED_20("notched_12"),
    NONE("none");

    private final String type;

    public String getType() {
        return type;
    }

    BossBarType(String type){
        this.type = type;
    }

    public static BossBarType valueOfString(String name){
        for(BossBarType type : values()){
            if(type.getType().equalsIgnoreCase(name)){
                return type;
            }
        }
        return FLAT;
    }
}
