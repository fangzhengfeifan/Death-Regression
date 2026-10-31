# Death Regression

死亡后，整个世界回退到你的存档点。

A Minecraft Forge mod for 1.20.1: when you die, the whole world rolls back to your last checkpoint.

> **仅支持单人游戏。不支持联机（局域网 / 专用服务器）。**
> **Singleplayer only. Multiplayer is not supported.**

---

## 这个模组做什么

你死了，不是简单地重生，而是**整个世界倒带**：

- 地形、方块、箱子、生物 —— 全部回到存档点创建时的状态
- 你本人带着满血出现在存档点位置
- 存档点之后你建造或修改的一切都会消失

人物数据（背包、经验等）默认**不保留**，跟随世界一起回退。可以在游戏内开启并按类别选择保留死亡那一刻的状态。

**这不是"重生点调整"，是完整的存档回滚。** 它会替换你的世界存档文件夹。

> ⚠️ 第一次使用前请备份你的世界。
> ⚠️ Back up your world before using this mod for the first time.

---

## 存档机制

| 触发方式 | 说明 |
|---|---|
| 睡觉 | 在床上睡觉时触发 |
| 游戏天数 | 每 N 个游戏日按概率触发（默认 10 天 / 50%） |
| 首次进入新世界 | 立刻创建初始存档点 |
| `/regression save` | 手动创建 |
| 指定物品右键 | 由 `manualSaveItem` 配置 |

- 两次自动存档之间有**冷却**（默认 100 分钟现实时间）
- 每个世界最多保留 **3 份**存档点，满了循环覆盖最旧的
- 回溯时使用**时间最新**的那一份
- 存档点绑定世界身份，删掉世界再建同名的会清理旧存档点

---

## 人物数据（共 11 项，可单独开关）

`keepPlayerData` 是总开关，默认关闭。打开后默认全保留，可逐项关闭：

| 配置项 | 内容 |
|---|---|
| `keepInventory` | 背包 36 格 |
| `keepArmor` | 装备（护甲 + 副手） |
| `keepEnderChest` | 末影箱 27 格 |
| `keepXp` | 经验 |
| `keepHunger` | 饥饿度 |
| `keepEffects` | 药水效果 |
| `keepAbsorption` | 伤害吸收 |
| `keepAir` | 氧气 |
| `keepScore` | 计分板分数 |
| `keepSpawnPoint` | 重生点 |
| `keepModData` | 模组数据（饰品等） |

**固定用存档点的**：位置、血量、着火、摔落距离、死亡状态。

---

## 指令

```
/regression config                      查看当前世界全部配置
/regression config playerdata           可点击的开关菜单
/regression config <项>                 查看单项
/regression config <项> true|false      修改单项
/regression save                        立刻创建存档点
/regression stats                       统计信息
/regression reload                      重读配置
```

需要权限等级 2（单人游戏需开作弊）。

---

## 配置位置

每个世界一份：

```
saves/<世界名>/serverconfig/regressionmod-server.toml
```

在 A 世界改的设置不影响 B 世界。

---

## 安装

1. 安装 Minecraft 1.20.1 + Forge 47.x
2. 把 `regressionmod-1.0.0.jar` 放进 `mods` 文件夹
3. 启动游戏

## 从源码构建

```bash
git clone https://github.com/fangzhengfeifan/Death-Regression.git
cd Death-Regression
./gradlew build          # Windows: gradlew.bat build
```

产物在 `build/libs/regressionmod-1.0.0.jar`。

需要 JDK 17。

---

## 已知问题

- 新建世界时，首次备份可能有少数区块文件未通过完整性校验。影响：回溯后这些区域可能显示为未生成。人物数据不受影响。
- 仅支持单人游戏。

---

## 许可

MIT，见 [LICENSE.txt](LICENSE.txt)。
