package io.github.crucible.timemachine.bossbar;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.UUID;

public abstract class BossBarComponent {

    private final UUID uuid;
    private ChatComponentText text;
    private BossBarType type;
    private BossBarColor color;
    private float percent;
    private boolean isVisible;

    public BossBarComponent(NBTTagCompound nbtTagCompound){
        this.uuid = UUID.fromString(nbtTagCompound.getString("uuid"));
        this.text = new ChatComponentText(nbtTagCompound.getString("text"));
        this.type = BossBarType.valueOfString(nbtTagCompound.getString("type"));
        this.color = BossBarColor.valueOfString(nbtTagCompound.getString("color"));
        this.isVisible = nbtTagCompound.getBoolean("isVisible");
        this.percent = nbtTagCompound.getFloat("percentage");
    }

    public BossBarComponent(UUID uuid){
        this.uuid = uuid;
        text = new ChatComponentText("");
        type = BossBarType.FLAT;
        color = BossBarColor.PINK;
        percent = 100F;
        isVisible = true;
    }

    public BossBarComponent(ChatComponentText text, BossBarColor color, BossBarType type, Float percent,boolean isVisible,UUID uuid){
        this.uuid = uuid;
        this.text = text;
        this.type = type;
        this.color = color;
        setPercent(percent);
        this.isVisible = isVisible;
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

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public String toString() {
        return "{\"Text\":\"" + text.getChatComponentText_TextValue() + "\",\"Color\":\""+color.getColor()+"\",\"Type\":\""+type.getType()+"\"}";
    }

    public NBTTagCompound toNbt(){
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.setString("text",text.getChatComponentText_TextValue());
        nbtTagCompound.setString("color",color.getColor());
        nbtTagCompound.setString("type",type.getType());
        nbtTagCompound.setFloat("percentage",percent);
        nbtTagCompound.setBoolean("isVisible",isVisible);
        nbtTagCompound.setString("uuid",uuid.toString());
        return nbtTagCompound;
    }
}
