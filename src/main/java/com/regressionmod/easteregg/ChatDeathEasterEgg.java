package com.regressionmod.easteregg;

import com.regressionmod.RegressionMod;
import com.regressionmod.config.ModConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Locale;

/**
 * 小彩蛋：在聊天框里喊出「死亡回归」的语言版本，会当场暴毙。
 *
 * 中文 / 英文 / 日文都认。判定是「包含」，不看大小写。
 */
public final class ChatDeathEasterEgg {

    /** 触发的关键词（包含即可）。 */
    private static final List<String> KEYWORDS = List.of(
            // 中文
            "死亡回归",
            "死亡回歸",
            // 英文
            "death regression",
            "return by death",
            "regression of death",
            // 日文
            "死亡回帰",
            "しぼうかいき",
            "死に戻り",
            "シニモドリ"
    );

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        if (!ModConfig.EASTER_EGG_ENABLED.get()) {
            return;
        }
        // 死亡回归关掉时不让彩蛋真的把人弄死（否则就是白白掉装备）
        if (!ModConfig.isDeathRegressionEnabled()) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }
        String text = event.getRawText() == null ? event.getMessage().getString() : event.getRawText();
        if (text == null) {
            return;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : KEYWORDS) {
            if (lower.contains(kw.toLowerCase(Locale.ROOT))) {
                trigger(player, kw);
                return;
            }
        }
    }

    private static void trigger(ServerPlayer player, String keyword) {
        RegressionMod.LOGGER.info("彩蛋触发：玩家 {} 在聊天里说了「{}」", player.getGameProfile().getName(), keyword);

        // 走 /kill 同款伤害类型，确保真的会死
        DamageSource kill = player.level().damageSources().genericKill();
        player.hurt(kill, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.kill();
        }
    }

    private ChatDeathEasterEgg() {}

    /** 供入口类构造（反编译代码与构造函数同名冲突时用工厂方法）。 */
    public static ChatDeathEasterEgg create() {
        return new ChatDeathEasterEgg();
    }
}
