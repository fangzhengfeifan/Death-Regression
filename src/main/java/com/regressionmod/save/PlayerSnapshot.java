/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 */
package com.regressionmod.save;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PlayerSnapshot {
    private final String playerUUID;
    private final String playerName;
    private final long gameTime;
    private final long realTime;
    private final double posX;
    private final double posY;
    private final double posZ;
    private final float rotationYaw;
    private final float rotationPitch;
    private final String dimensionId;
    private final float health;
    private final float maxHealth;
    private final int foodLevel;
    private final float foodSaturation;
    private final int experienceLevel;
    private final float experienceProgress;
    private final int totalExperience;
    private final ListTag inventory;
    private final ListTag armorItems;
    private final ListTag offhandItems;
    private final ListTag enderItems;
    private final CompoundTag activeEffects;
    private final long dayTime;
    private final boolean raining;
    private final boolean thundering;
    private final CompoundTag additionalData;

    public PlayerSnapshot(ServerPlayer player) {
        CompoundTag itemTag;
        ItemStack stack;
        int i;
        this.playerUUID = player.getUUID().toString();
        this.playerName = player.getName().getString();
        this.gameTime = player.level().getGameTime();
        this.realTime = System.currentTimeMillis();
        this.posX = player.getX();
        this.posY = player.getY();
        this.posZ = player.getZ();
        this.rotationYaw = player.getYRot();
        this.rotationPitch = player.getXRot();
        this.dimensionId = player.level().dimension().location().toString();
        this.health = player.getHealth();
        this.maxHealth = player.getMaxHealth();
        this.foodLevel = player.getFoodData().getFoodLevel();
        this.foodSaturation = player.getFoodData().getSaturationLevel();
        this.experienceLevel = player.experienceLevel;
        this.experienceProgress = player.experienceProgress;
        this.totalExperience = player.totalExperience;
        this.inventory = new ListTag();
        this.armorItems = new ListTag();
        this.offhandItems = new ListTag();
        for (i = 0; i < 36; ++i) {
            stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            stack.save(itemTag);
            this.inventory.add(itemTag);
        }
        for (i = 36; i < 40; ++i) {
            stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            itemTag = new CompoundTag();
            itemTag.putInt("Slot", i - 36);
            stack.save(itemTag);
            this.armorItems.add(itemTag);
        }
        ItemStack offhandStack = player.getInventory().getItem(40);
        if (!offhandStack.isEmpty()) {
            CompoundTag itemTag2 = new CompoundTag();
            itemTag2.putInt("Slot", 0);
            offhandStack.save(itemTag2);
            this.offhandItems.add(itemTag2);
        }
        this.enderItems = new ListTag();
        for (int i2 = 0; i2 < player.getEnderChestInventory().getContainerSize(); ++i2) {
            ItemStack stack2 = player.getEnderChestInventory().getItem(i2);
            if (stack2.isEmpty()) continue;
            CompoundTag itemTag3 = new CompoundTag();
            itemTag3.putInt("Slot", i2);
            stack2.save(itemTag3);
            this.enderItems.add(itemTag3);
        }
        this.activeEffects = new CompoundTag();
        ListTag effects = new ListTag();
        player.getActiveEffects().forEach(effect -> effects.add(effect.save(new CompoundTag())));
        this.activeEffects.put("ActiveEffects", (Tag)effects);
        Level level = player.level();
        this.dayTime = level.getDayTime();
        this.raining = level.isRaining();
        this.thundering = level.isThundering();
        this.additionalData = new CompoundTag();
        player.addAdditionalSaveData(this.additionalData);
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("PlayerUUID", this.playerUUID);
        tag.putString("PlayerName", this.playerName);
        tag.putLong("GameTime", this.gameTime);
        tag.putLong("RealTime", this.realTime);
        tag.putDouble("PosX", this.posX);
        tag.putDouble("PosY", this.posY);
        tag.putDouble("PosZ", this.posZ);
        tag.putFloat("RotationYaw", this.rotationYaw);
        tag.putFloat("RotationPitch", this.rotationPitch);
        tag.putString("DimensionId", this.dimensionId);
        tag.putFloat("Health", this.health);
        tag.putFloat("MaxHealth", this.maxHealth);
        tag.putInt("FoodLevel", this.foodLevel);
        tag.putFloat("FoodSaturation", this.foodSaturation);
        tag.putInt("ExperienceLevel", this.experienceLevel);
        tag.putFloat("ExperienceProgress", this.experienceProgress);
        tag.putInt("TotalExperience", this.totalExperience);
        tag.put("Inventory", (Tag)this.inventory);
        tag.put("ArmorItems", (Tag)this.armorItems);
        tag.put("OffhandItems", (Tag)this.offhandItems);
        tag.put("EnderItems", (Tag)this.enderItems);
        tag.put("ActiveEffects", (Tag)this.activeEffects);
        tag.putLong("DayTime", this.dayTime);
        tag.putBoolean("Raining", this.raining);
        tag.putBoolean("Thundering", this.thundering);
        tag.put("AdditionalData", (Tag)this.additionalData);
        return tag;
    }

    public static PlayerSnapshot deserialize(CompoundTag tag) {
        return new PlayerSnapshot(tag);
    }

    private PlayerSnapshot(CompoundTag tag) {
        this.playerUUID = tag.getString("PlayerUUID");
        this.playerName = tag.getString("PlayerName");
        this.gameTime = tag.getLong("GameTime");
        this.realTime = tag.getLong("RealTime");
        this.posX = tag.getDouble("PosX");
        this.posY = tag.getDouble("PosY");
        this.posZ = tag.getDouble("PosZ");
        this.rotationYaw = tag.getFloat("RotationYaw");
        this.rotationPitch = tag.getFloat("RotationPitch");
        this.dimensionId = tag.getString("DimensionId");
        this.health = tag.getFloat("Health");
        this.maxHealth = tag.getFloat("MaxHealth");
        this.foodLevel = tag.getInt("FoodLevel");
        this.foodSaturation = tag.getFloat("FoodSaturation");
        this.experienceLevel = tag.getInt("ExperienceLevel");
        this.experienceProgress = tag.getFloat("ExperienceProgress");
        this.totalExperience = tag.getInt("TotalExperience");
        this.inventory = tag.getList("Inventory", 10);
        this.armorItems = tag.getList("ArmorItems", 10);
        this.offhandItems = tag.getList("OffhandItems", 10);
        this.enderItems = tag.getList("EnderItems", 10);
        this.activeEffects = tag.getCompound("ActiveEffects");
        this.dayTime = tag.getLong("DayTime");
        this.raining = tag.getBoolean("Raining");
        this.thundering = tag.getBoolean("Thundering");
        this.additionalData = tag.getCompound("AdditionalData");
    }

    public String getPlayerUUID() {
        return this.playerUUID;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public long getGameTime() {
        return this.gameTime;
    }

    public long getRealTime() {
        return this.realTime;
    }

    public double getPosX() {
        return this.posX;
    }

    public double getPosY() {
        return this.posY;
    }

    public double getPosZ() {
        return this.posZ;
    }

    public float getRotationYaw() {
        return this.rotationYaw;
    }

    public float getRotationPitch() {
        return this.rotationPitch;
    }

    public String getDimensionId() {
        return this.dimensionId;
    }

    public float getHealth() {
        return this.health;
    }

    public float getMaxHealth() {
        return this.maxHealth;
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public float getFoodSaturation() {
        return this.foodSaturation;
    }

    public int getExperienceLevel() {
        return this.experienceLevel;
    }

    public float getExperienceProgress() {
        return this.experienceProgress;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public ListTag getInventory() {
        return this.inventory;
    }

    public ListTag getArmorItems() {
        return this.armorItems;
    }

    public ListTag getOffhandItems() {
        return this.offhandItems;
    }

    public ListTag getEnderItems() {
        return this.enderItems;
    }

    public CompoundTag getActiveEffects() {
        return this.activeEffects;
    }

    public long getDayTime() {
        return this.dayTime;
    }

    public boolean isRaining() {
        return this.raining;
    }

    public boolean isThundering() {
        return this.thundering;
    }

    public CompoundTag getAdditionalData() {
        return this.additionalData;
    }

    public BlockPos getBlockPos() {
        return BlockPos.containing((double)this.posX, (double)this.posY, (double)this.posZ);
    }
}

