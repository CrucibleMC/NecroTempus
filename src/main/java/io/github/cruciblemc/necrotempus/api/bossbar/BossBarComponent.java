package io.github.cruciblemc.necrotempus.api.bossbar;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public abstract class BossBarComponent extends TimedElement {

    private final UUID uuid;

    private ChatComponentText text;
    private BossBarColor color;
    private BossBarType type;
    private float percentage;
    private boolean isVisible;

    private int lazyColor = -1;

    protected BossBarComponent(NBTTagCompound nbtTagCompound) {
        this.uuid = UUID.fromString(nbtTagCompound.getString("uuid"));
        this.text = new ChatComponentText(nbtTagCompound.getString("text"));
        this.type = BossBarType.valueOfString(nbtTagCompound.getString("type"));
        this.color = BossBarColor.valueOfString(nbtTagCompound.getString("color"));
        this.isVisible = nbtTagCompound.getBoolean("isVisible");
        this.percentage = nbtTagCompound.getFloat("percentage");
    }

    protected BossBarComponent(UUID uuid) {
        this(uuid, new ChatComponentText(""), BossBarColor.PINK, BossBarType.FLAT, 1F, true);
    }

    protected BossBarComponent() {
        this(UUID.randomUUID());
    }

    protected BossBarComponent(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type, Float percent, boolean isVisible) {
        this.uuid = uuid;
        this.text = text;
        this.type = type;
        this.color = color;
        setPercentage(percent);
        this.isVisible = isVisible;
    }

    public NBTTagCompound toNbt() {

        NBTTagCompound nbtTagCompound = new NBTTagCompound();

        nbtTagCompound.setString("text", text.getChatComponentText_TextValue());
        nbtTagCompound.setString("type", type.getType());
        nbtTagCompound.setFloat("percentage", percentage);
        nbtTagCompound.setBoolean("isVisible", isVisible);
        nbtTagCompound.setString("uuid", uuid.toString());

        if (color == BossBarColor.LAZY) {
            nbtTagCompound.setString("color", "$" + color.intValue());
        } else {
            nbtTagCompound.setString("color", color.getIdentifier());
        }

        return nbtTagCompound;
    }
}
