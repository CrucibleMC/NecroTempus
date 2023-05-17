package io.github.crucible.necrotempus.modules.bossbar.internal.component;

import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarColor;
import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.UUID;

import static io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarColor.LAZY;
import static io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarColor.PINK;
import static io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarType.FLAT;

public abstract class BossBarComponent {

    private final UUID uuid;

    private ChatComponentText text;
    private BossBarColor color;
    private BossBarType type;
    private float percentage;
    private boolean isVisible;

    public BossBarComponent(NBTTagCompound nbtTagCompound){
        this.uuid = UUID.fromString(nbtTagCompound.getString("uuid"));
        this.text = new ChatComponentText(nbtTagCompound.getString("text"));
        this.type = BossBarType.valueOfString(nbtTagCompound.getString("type"));
        this.color = BossBarColor.valueOfString(nbtTagCompound.getString("color"));
        this.isVisible = nbtTagCompound.getBoolean("isVisible");
        this.percentage = nbtTagCompound.getFloat("percentage");
    }

    public BossBarComponent(UUID uuid){
        this(uuid, new ChatComponentText(""), PINK, FLAT, 1F, true);
    }

    public BossBarComponent(){
        this(UUID.randomUUID());
    }

    public BossBarComponent(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type, Float percent,boolean isVisible){
        this.uuid = uuid;
        this.text = text;
        this.type = type;
        this.color = color;
        setPercentage(percent);
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

    public float getPercentage() {
        return percentage;
    }

    public void setPercentage(float percentage) {
        this.percentage = Math.max(0F, Math.min(1F, percentage));
    }

    public void setVisible(boolean visibility) {
        this.isVisible = visibility;
    }

    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public String toString() {
        return "BossBarComponent{" +
                "uuid=" + uuid +
                ", text=" + text +
                ", color=" + color +
                ", type=" + type +
                ", percentage=" + percentage +
                ", isVisible=" + isVisible +
                '}';
    }

    public NBTTagCompound toNbt(){

        NBTTagCompound nbtTagCompound = new NBTTagCompound();

        nbtTagCompound.setString(   "text",         text.getChatComponentText_TextValue());
        nbtTagCompound.setString(   "type",         type.getType());
        nbtTagCompound.setFloat(    "percentage",   percentage);
        nbtTagCompound.setBoolean(  "isVisible",    isVisible);
        nbtTagCompound.setString(   "uuid",         uuid.toString());

        if(color == LAZY){
            nbtTagCompound.setString(   "color",        "$" + color.intValue());
        }else{
            nbtTagCompound.setString(   "color",        color.getIdentifier());
        }

        return nbtTagCompound;
    }
}
