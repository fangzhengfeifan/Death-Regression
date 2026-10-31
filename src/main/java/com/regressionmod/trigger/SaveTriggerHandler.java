/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.entity.player.PlayerSleepInBedEvent
 *  net.minecraftforge.eventbus.api.Event
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 */
package com.regressionmod.trigger;

import com.regressionmod.RegressionMod;
import com.regressionmod.backup.BackupManager;
import com.regressionmod.config.ModConfig;
import com.regressionmod.event.RegressionEvents;
import com.regressionmod.save.PlayerDataTracker;
import com.regressionmod.trigger.DayCounter;
import com.regressionmod.trigger.SafetyChecker;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SaveTriggerHandler {
    private final Random random = new Random();
    private BackupManager backupManager;

    public void setBackupManager(BackupManager mgr) {
        this.backupManager = mgr;
    }

    public void setDayCounter(DayCounter dc) {
        dc.setSaveTriggerHandler(this);
    }

    @SubscribeEvent
    public void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
        String mode = ((String)ModConfig.SAVE_MODE.get()).toUpperCase();
        if (!mode.equals("SLEEP") && !mode.equals("MIXED")) {
            return;
        }
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        this.tryTriggerSave(player2, "SLEEP");
    }

    public void onDayTrigger(MinecraftServer server) {
        String mode = ((String)ModConfig.SAVE_MODE.get()).toUpperCase();
        if (!mode.equals("DAYS") && !mode.equals("MIXED")) {
            return;
        }
        if (this.random.nextDouble() >= (Double)ModConfig.DAYS_PROBABILITY.get()) {
            RegressionMod.LOGGER.info("\u5929\u6570\u6982\u7387\u672a\u901a\u8fc7\uff0c\u8df3\u8fc7");
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!this.tryTriggerSave(player, "DAYS")) continue;
            return;
        }
    }

    /** 玩家手持配置物品右键：手动存档。物品为空则不生效。 */
    @SubscribeEvent
    public void onItemUse(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        String itemId = (String)ModConfig.MANUAL_SAVE_ITEM.get();
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getLevel().isClientSide) {
            return;
        }
        net.minecraft.resources.ResourceLocation want = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        if (want == null) {
            return;
        }
        net.minecraft.resources.ResourceLocation got =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (got == null || !got.equals(want)) {
            return;
        }
        this.tryTriggerSave(player, "ITEM");
    }

    private boolean tryTriggerSave(ServerPlayer player, String triggerType) {        MinecraftServer server = player.getServer();
        if (server == null || this.backupManager == null) {
            return false;
        }
        if (!SafetyChecker.isSafeToSave(player)) {
            this.sendMsg(player, this.getUnsafeReason(player), ChatFormatting.RED);
            return false;
        }
        int cooldown = (Integer)ModConfig.SAVE_COOLDOWN_MINUTES.get();
        PlayerDataTracker tracker = PlayerDataTracker.get(server);
        if (!tracker.isCooldownPassed(cooldown)) {
            String rem = tracker.getCooldownRemainingFormatted(cooldown);
            this.sendMsg(player, "\u5b58\u6863\u51b7\u5374\u4e2d\uff0c\u8fd8\u9700\u7b49\u5f85 " + rem, ChatFormatting.YELLOW);
            return false;
        }
        RegressionEvents.BeforeAutoSaveEvent beforeEv = new RegressionEvents.BeforeAutoSaveEvent(player, (ResourceKey<Level>)player.level().dimension(), triggerType);
        if (MinecraftForge.EVENT_BUS.post((Event)beforeEv)) {
            return false;
        }
        this.sendMsg(player, "\u00a7a[\u81ea\u52a8\u5b58\u6863] \u6b63\u5728\u4fdd\u5b58\u4e16\u754c...", ChatFormatting.GREEN);
        boolean ok = this.backupManager.createBackup(server, player, triggerType);
        if (ok) {
            this.sendMsg(player, "\u00a7a[\u81ea\u52a8\u5b58\u6863\u5b8c\u6210] \u4e16\u754c\u5df2\u4fdd\u5b58\uff01", ChatFormatting.GREEN);
            MinecraftForge.EVENT_BUS.post((Event)new RegressionEvents.AfterAutoSaveEvent(player, (ResourceKey<Level>)player.level().dimension(), triggerType));
        } else {
            this.sendMsg(player, "\u00a7c[\u81ea\u52a8\u5b58\u6863\u5931\u8d25] \u4fdd\u5b58\u4e16\u754c\u65f6\u51fa\u9519\uff01", ChatFormatting.RED);
        }
        return ok;
    }

    private String getUnsafeReason(ServerPlayer player) {
        if (((Boolean)ModConfig.REQUIRE_FULL_HEALTH.get()).booleanValue() && !SafetyChecker.isFullHealth(player)) {
            return "\u751f\u547d\u503c\u672a\u6ee1";
        }
        if (!SafetyChecker.isNotInCombat(player)) {
            return "\u5904\u4e8e\u6218\u6597\u72b6\u6001";
        }
        if (!SafetyChecker.isOnGround(player)) {
            return "\u4e0d\u5728\u5b89\u5168\u5730\u9762";
        }
        if (SafetyChecker.isDimensionBlacklisted((ResourceKey<Level>)player.level().dimension())) {
            return "\u6b64\u7ef4\u5ea6\u4e0d\u5141\u8bb8\u5b58\u6863";
        }
        return "\u4e0d\u6ee1\u8db3\u5b58\u6863\u6761\u4ef6";
    }

    private void sendMsg(ServerPlayer player, String msg, ChatFormatting color) {
        player.sendSystemMessage((Component)Component.literal((String)msg).withStyle(color));
    }
}

