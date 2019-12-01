package io.github.crucible.timemachine.bossbar;

import net.minecraft.util.ChatComponentText;

import java.util.UUID;

public abstract class BossBarComponent {

    private final UUID uuid;
    private ChatComponentText text;
    private BossBarType type;
    private BossBarColor color;
    private float percent;

    public BossBarComponent(UUID uuid){
        this.uuid = uuid;
        text = new ChatComponentText("");
        type = BossBarType.FLAT;
        color = BossBarColor.PINK;
        percent = 100F;
    }

    public BossBarComponent(ChatComponentText text, BossBarColor color, BossBarType type, Float percent, UUID uuid){
        this.uuid = uuid;
        this.text = text;
        this.type = type;
        this.color = color;
        setPercent(percent);
    }

    public UUID getUuid() {
        return uuid;
    }

    public BossBarColor getColor() {
        return color;
    }

    public BossBarType getType() {
        return type;
    }

    public ChatComponentText getText() {
        return text;
    }

    public void setColor(BossBarColor color) {
        this.color = color;
    }

    public void setText(ChatComponentText text) {
        this.text = text;
    }

    public void setType(BossBarType type) {
        this.type = type;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = Math.min(100F, Math.max(percent,1F));
    }

    @Override
    public String toString() {
        return "{\"Text\":\"" + text.getChatComponentText_TextValue() + "\",\"Color\":\""+color.getColor()+"\",\"Type\":\""+type.getType()+"\"}";
    }
}
