/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  net.minecraft.ChatFormatting
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraftforge.event.RegisterCommandsEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.regressionmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.regressionmod.RegressionMod;
import com.regressionmod.api.RegressionAPI;
import com.regressionmod.backup.BackupManager;
import com.regressionmod.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="regressionmod", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class RegressionCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher d = event.getDispatcher();
        d.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"regression").then(((LiteralArgumentBuilder)Commands.literal((String)"stats").requires(s -> s.hasPermission(0))).executes(ctx -> RegressionCommand.stats((CommandSourceStack)ctx.getSource())))).then(((LiteralArgumentBuilder)Commands.literal((String)"save").requires(s -> s.hasPermission(2))).executes(ctx -> RegressionCommand.save((CommandSourceStack)ctx.getSource())))).then(((LiteralArgumentBuilder)Commands.literal((String)"reload").requires(s -> s.hasPermission(2))).executes(ctx -> RegressionCommand.reload((CommandSourceStack)ctx.getSource()))));
        RegressionConfigCommand.register(d);
    }

    private static int stats(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure((Component)Component.literal((String)"\u6b64\u547d\u4ee4\u53ea\u80fd\u5728\u6e38\u620f\u4e2d\u4f7f\u7528").withStyle(ChatFormatting.RED));
            return 0;
        }
        BackupManager bm = RegressionMod.getBackupManager();
        int deaths = RegressionAPI.getDeathCount((Player)player);
        int regs = RegressionAPI.getRegressionCount((Player)player);
        long lastSave = RegressionAPI.getLastSaveTime(player.level());
        String lastSaveStr = lastSave <= 0L ? "\u4ece\u672a\u5b58\u6863" : (System.currentTimeMillis() - lastSave) / 60000L + " \u5206\u949f\u524d";
        String worldPath = bm != null && bm.getWorldDir() != null ? bm.getWorldDir().toAbsolutePath().toString() : "\u672a\u77e5";
        String backupPath = bm != null && bm.getBackupRootDir() != null ? bm.getBackupRootDir().toAbsolutePath().toString() : "\u672a\u77e5";
        boolean hasBk = bm != null && bm.hasBackup();
        source.sendSuccess(() -> Component.literal((String)"").append((Component)Component.literal((String)"========== \u56de\u5f52\u6a21\u7ec4\u7edf\u8ba1 ==========\n").withStyle(ChatFormatting.GOLD)).append((Component)Component.literal((String)("\u6b7b\u4ea1: " + deaths + " \u6b21")).withStyle(ChatFormatting.RED)).append((Component)Component.literal((String)("  \u56de\u6eaf: " + regs + " \u6b21")).withStyle(ChatFormatting.GREEN)).append((Component)Component.literal((String)("  \u4e0a\u6b21\u5b58\u6863: " + lastSaveStr)).withStyle(ChatFormatting.YELLOW)).append((Component)Component.literal((String)("\n\u53ef\u7528\u5907\u4efd: " + (hasBk ? "\u662f" : "\u5426"))).withStyle(hasBk ? ChatFormatting.GREEN : ChatFormatting.RED)).append((Component)Component.literal((String)("\n\u4e16\u754c: " + worldPath)).withStyle(ChatFormatting.GRAY)).append((Component)Component.literal((String)("\n\u5907\u4efd: " + backupPath)).withStyle(ChatFormatting.GRAY)).append((Component)Component.literal((String)"\n==================================").withStyle(ChatFormatting.GOLD)), false);
        return 1;
    }

    private static int save(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure((Component)Component.literal((String)"\u6b64\u547d\u4ee4\u53ea\u80fd\u5728\u6e38\u620f\u4e2d\u4f7f\u7528").withStyle(ChatFormatting.RED));
            return 0;
        }
        BackupManager bm = RegressionMod.getBackupManager();
        if (bm == null) {
            source.sendFailure((Component)Component.literal((String)"\u5907\u4efd\u7ba1\u7406\u5668\u672a\u521d\u59cb\u5316").withStyle(ChatFormatting.RED));
            return 0;
        }
        boolean ok = bm.createBackup(player.getServer(), player, "MANUAL");
        if (ok) {
            source.sendSuccess(() -> Component.literal((String)"\u624b\u52a8\u5b58\u6863\u6210\u529f").withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        source.sendFailure((Component)Component.literal((String)"\u624b\u52a8\u5b58\u6863\u5931\u8d25").withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int reload(CommandSourceStack source) {
        ModConfig.load();
        source.sendSuccess(() -> Component.literal((String)"\u914d\u7f6e\u5df2\u91cd\u8f7d").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}

