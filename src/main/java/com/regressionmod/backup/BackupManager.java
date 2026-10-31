/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 */
package com.regressionmod.backup;

import com.regressionmod.RegressionMod;
import com.regressionmod.config.ModConfig;
import com.regressionmod.save.PlayerDataTracker;
import com.regressionmod.save.PlayerSnapshot;
import com.regressionmod.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class BackupManager {
    private Path worldDir;
    private Path backupRootDir;
    private volatile boolean isWorking;
    /** 当前世界的身份（随机 UUID），用来认领备份，防止用错世界的存档点。 */
    private String worldId;
    /** 世界自己的身份，存在世界目录里。 */
    private static final String ID_FILE = ".regression_world_id";
    /** 备份里的身份文件名。 */
    private static final String BACKUP_ID_FILE = "world_id.txt";

    public void initialize(MinecraftServer server) {
        Path serverDir = server.getServerDirectory().toPath();
        RegressionMod.LOGGER.info("\u670d\u52a1\u5668\u76ee\u5f55: {}", (Object)serverDir);
        RegressionMod.LOGGER.info("\u4e16\u754c\u5185\u90e8\u540d\u79f0: {}", (Object)server.getWorldData().getLevelName());
        // 第一优先：服务器自己给出的世界目录。这是唯一不会认错世界的来源。
        Path fromServer = null;
        try {
            fromServer = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .toAbsolutePath().normalize();
        } catch (Throwable ignored) {
            // 老版本/特殊端可能没有这个 API，往下走兜底
        }
        if (fromServer != null && Files.exists(fromServer.resolve("level.dat"), new LinkOption[0])) {
            this.worldDir = fromServer;
            RegressionMod.LOGGER.info("\u4e16\u754c\u76ee\u5f55\uff08\u670d\u52a1\u5668\u81ea\u62a5\uff09: {}", (Object)this.worldDir);
        } else if (Files.exists(serverDir.resolve("level.dat"), new LinkOption[0])) {
            this.worldDir = serverDir;
            RegressionMod.LOGGER.info("\u4e16\u754c\u76ee\u5f55\uff08\u72ec\u7acb\u6a21\u5f0f\uff09: {}", (Object)this.worldDir);
        } else {
            this.worldDir = this.findWorldInSaves(serverDir);
            RegressionMod.LOGGER.info("\u4e16\u754c\u76ee\u5f55\uff08saves \u626b\u63cf\uff09: {}", (Object)this.worldDir);
        }
        if (this.worldDir == null || !Files.exists(this.worldDir.resolve("level.dat"), new LinkOption[0])) {
            RegressionMod.LOGGER.error("\u65e0\u6cd5\u627e\u5230\u6709\u6548\u4e16\u754c\u76ee\u5f55\uff01\u964d\u7ea7\u4f7f\u7528\u670d\u52a1\u5668\u76ee\u5f55");
            this.worldDir = serverDir;
        }
        Path parentOfWorld = this.worldDir.getParent();
        String folderName = this.worldDir.getFileName().toString();
        this.backupRootDir = parentOfWorld.resolve("regression_backups").resolve(folderName);
        try {
            Files.createDirectories(this.backupRootDir, new FileAttribute[0]);
        }
        catch (IOException e) {
            RegressionMod.LOGGER.error("\u65e0\u6cd5\u521b\u5efa\u5907\u4efd\u76ee\u5f55", (Throwable)e);
        }
        RegressionMod.LOGGER.info("\u4e16\u754c\u6587\u4ef6\u5939: {}", (Object)this.worldDir.toAbsolutePath());
        RegressionMod.LOGGER.info("\u5907\u4efd\u6839\u76ee\u5f55: {}", (Object)this.backupRootDir.toAbsolutePath());
        this.loadOrCreateWorldId();
        this.dropForeignBackups();
    }

    /**
     * 认出"这个世界是谁"。世界目录里存一个随机 ID；
     * 备份目录里也存一份，两边对不上就说明那份备份是别的世界留下的。
     */
    private void loadOrCreateWorldId() {
        Path idFile = this.worldDir.resolve(ID_FILE);
        try {
            if (Files.exists(idFile, new LinkOption[0])) {
                String text = new String(Files.readAllBytes(idFile), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!text.isEmpty()) {
                    this.worldId = text;
                    RegressionMod.LOGGER.info("\u4e16\u754c\u8eab\u4efd: {}", (Object)this.worldId);
                    return;
                }
            }
            this.worldId = java.util.UUID.randomUUID().toString();
            Files.write(idFile, this.worldId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            RegressionMod.LOGGER.info("\u4e16\u754c\u8eab\u4efd\uff08\u65b0\u5efa\uff09: {}", (Object)this.worldId);
        } catch (Exception e) {
            this.worldId = null;
            RegressionMod.LOGGER.warn("\u4e16\u754c\u8eab\u4efd\u8bfb\u5199\u5931\u8d25\uff0c\u672c\u6b21\u4e0d\u6821\u9a8c\u5907\u4efd\u5f52\u5c5e", (Throwable)e);
        }
    }

    /** 备份目录里那些不属于当前世界的存档点，删掉（世界被删掉重建时会留下这种东西）。 */
    private void dropForeignBackups() {
        if (this.worldId == null || this.backupRootDir == null || !Files.isDirectory(this.backupRootDir)) {
            return;
        }
        try (Stream<Path> dirs = Files.list(this.backupRootDir)) {
            for (Path dir : dirs.toList()) {
                if (!Files.isDirectory(dir, new LinkOption[0])
                        || !dir.getFileName().toString().startsWith("backup_")) {
                    continue;
                }
                Path idFile = dir.resolve(BACKUP_ID_FILE);
                String owner = null;
                if (Files.exists(idFile, new LinkOption[0])) {
                    owner = new String(Files.readAllBytes(idFile), java.nio.charset.StandardCharsets.UTF_8).trim();
                }
                if (owner == null || !owner.equals(this.worldId)) {
                    RegressionMod.LOGGER.warn("\u5220\u9664\u4e0d\u5c5e\u4e8e\u672c\u4e16\u754c\u7684\u65e7\u5b58\u6863\u70b9: {}", (Object)dir.getFileName());
                    FileUtils.deleteDirectory(dir);
                }
            }
        } catch (Exception e) {
            RegressionMod.LOGGER.warn("\u6e05\u7406\u65e7\u5b58\u6863\u70b9\u5931\u8d25", (Throwable)e);
        }
    }

    private Path findWorldInSaves(Path serverDir) {
        Path[] possibleSaves;
        for (Path savesDir : possibleSaves = new Path[]{serverDir.resolve("saves"), serverDir.getParent() != null ? serverDir.getParent().resolve("saves") : null}) {
            if (savesDir == null || !Files.exists(savesDir, new LinkOption[0])) continue;
            RegressionMod.LOGGER.info("\u626b\u63cf saves \u76ee\u5f55\uff0c\u627e\u6700\u8fd1\u4fee\u6539\u7684\u4e16\u754c: {}", (Object)savesDir);
            Path best = null;
            long bestTime = 0L;
            try (Stream<Path> dirs = Files.list(savesDir);){
                for (Path dir : dirs.toList()) {
                    Path levelDat;
                    if (!Files.isDirectory(dir, new LinkOption[0]) || !Files.exists(levelDat = dir.resolve("level.dat"), new LinkOption[0])) continue;
                    long modTime = dir.toFile().lastModified();
                    RegressionMod.LOGGER.info("  \u5019\u9009: {} (\u6700\u540e\u4fee\u6539: {})", (Object)dir.getFileName(), (Object)modTime);
                    if (modTime <= bestTime) continue;
                    bestTime = modTime;
                    best = dir;
                }
            }
            catch (IOException iOException) {
                // empty catch block
            }
            if (best == null) continue;
            RegressionMod.LOGGER.info("\u9009\u4e2d\u4e16\u754c: {} (\u4fee\u6539\u65f6\u95f4: {})", (Object)best.getFileName(), (Object)bestTime);
            return best;
        }
        RegressionMod.LOGGER.warn("\u626b\u63cf\u5931\u8d25\uff0c\u4f7f\u7528\u964d\u7ea7\u65b9\u6848");
        return serverDir.resolve("saves").resolve("world");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean createBackup(MinecraftServer server, ServerPlayer player, String triggerType) {
        if (this.isWorking) {
            RegressionMod.LOGGER.warn("\u5df2\u6709\u64cd\u4f5c\u5728\u8fdb\u884c\u4e2d");
            return false;
        }
        if (this.backupRootDir == null || this.worldDir == null) {
            RegressionMod.LOGGER.error("\u5907\u4efd\u7ba1\u7406\u5668\u672a\u521d\u59cb\u5316");
            return false;
        }
        this.isWorking = true;
        long start = System.currentTimeMillis();
        try {
            server.saveEverything(true, true, true);
            int maxBackups = (Integer)ModConfig.MAX_BACKUPS.get();
            int nextIdx = this.nextBackupIndex();
            Path target = this.backupRootDir.resolve("backup_" + nextIdx);
            if (Files.exists(target, new LinkOption[0])) {
                FileUtils.deleteDirectory(target);
            }
            Files.createDirectories(target, new FileAttribute[0]);
            RegressionMod.LOGGER.info("\u5907\u4efd: {} -> {}", (Object)this.worldDir, (Object)target);
            this.copyWorldVerified(this.worldDir, target);
            if (this.worldId != null) {
                Files.write(target.resolve(BACKUP_ID_FILE), this.worldId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            if (player != null) {
                PlayerSnapshot snap = new PlayerSnapshot(player);
                NbtIo.writeCompressed((CompoundTag)snap.serialize(), (File)target.resolve("snapshot.nbt").toFile());
            }
            this.cleanupOldBackups(maxBackups);
            PlayerDataTracker.get(server).recordSaveTime();
            long elapsed = System.currentTimeMillis() - start;
            RegressionMod.LOGGER.info("\u5907\u4efd #{} \u5b8c\u6210 ({}ms), \u89e6\u53d1: {}", new Object[]{nextIdx, elapsed, triggerType});
            return true;
        }
        catch (Exception e) {
            RegressionMod.LOGGER.error("\u5907\u4efd\u5931\u8d25: {}", (Object)e.getMessage(), (Object)e);
            return false;
        }
        finally {
            this.isWorking = false;
        }
    }

    /**
     * 复制世界，并保证区块文件完整。
     *
     * <p>区块是<b>异步</b>写盘的：{@code saveEverything} 返回之后，后台线程还在往
     * {@code .mca} 里写，能持续好几秒。这时候直接复制就会抄到写了一半的文件
     * （长度不是 4096 的倍数，区块索引指向文件尾之外），回档后那些方块读不出来。
     *
     * <p>所以：先等文件大小连续两次采样都不再变化，再复制；复制完逐个校验，
     * 对不上就再等稳定、再重抄。
     */
    private void copyWorldVerified(Path src, Path dst) throws IOException {
        waitForStableChunks(src);
        FileUtils.copyDirectory(src, dst);
        for (int attempt = 0; attempt < 3; ++attempt) {
            java.util.List<Path> bad = findBadChunkFiles(src, dst);
            if (bad.isEmpty()) {
                return;
            }
            RegressionMod.LOGGER.warn("\u6709 {} \u4e2a\u533a\u5757\u6587\u4ef6\u6ca1\u6284\u5b8c\u6574\uff0c\u7b49\u5199\u76d8\u505c\u4e0b\u6765\u91cd\u6284\uff08\u7b2c {} \u6b21\uff09", bad.size(), attempt + 1);
            waitForStableChunks(src);
            for (Path p : bad) {
                Path rel = src.relativize(p);
                Files.copy(p, dst.resolve(rel), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        java.util.List<Path> still = findBadChunkFiles(src, dst);
        if (!still.isEmpty()) {
            RegressionMod.LOGGER.error("\u4ecd\u6709 {} \u4e2a\u533a\u5757\u6587\u4ef6\u4e0d\u5b8c\u6574\uff0c\u8fd9\u4efd\u5907\u4efd\u53ef\u80fd\u4e0d\u53ef\u9760", still.size());
        }
    }

    /**
     * 等到全世界所有 {@code .mca} 的大小连续两次采样都不变（最多等 15 秒）。
     *
     * <p>主线程在这里是阻塞的，所以游戏不会再产生新的区块写入，只会等后台把已有的写完。
     */
    private static void waitForStableChunks(Path src) {
        long deadline = System.currentTimeMillis() + 15000L;
        java.util.Map<String, Long> prev = chunkSizes(src);
        int samples = 0;
        while (System.currentTimeMillis() < deadline) {
            sleepQuietly(500L);
            java.util.Map<String, Long> now = chunkSizes(src);
            if (now.equals(prev)) {
                if (samples > 0) {
                    RegressionMod.LOGGER.debug("\u533a\u5757\u5199\u76d8\u5df2\u505c\u4e0b\uff0c\u5f00\u59cb\u590d\u5236");
                }
                return;
            }
            prev = now;
            samples++;
        }
        RegressionMod.LOGGER.warn("\u7b49\u4e86 15 \u79d2\u533a\u5757\u6587\u4ef6\u4ecd\u5728\u53d8\u5316\uff0c\u76f4\u63a5\u5c1d\u8bd5\u590d\u5236");
    }

    private static java.util.Map<String, Long> chunkSizes(Path root) {
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.toList()) {
                if (!p.getFileName().toString().endsWith(".mca")) {
                    continue;
                }
                try {
                    map.put(root.relativize(p).toString(), Files.size(p));
                } catch (IOException ignored) {
                    // 读不到就当不存在
                }
            }
        } catch (IOException ignored) {
            // 忽略
        }
        return map;
    }

    /** 找出没抄好的 .mca（大小不一致，或者不是 4096 的整数倍）。 */
    private static java.util.List<Path> findBadChunkFiles(Path src, Path dst) {
        java.util.List<Path> bad = new java.util.ArrayList<>();
        try (Stream<Path> walk = Files.walk(src)) {
            for (Path p : walk.toList()) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".mca")) {
                    continue;
                }
                Path mirror = dst.resolve(src.relativize(p));
                try {
                    if (!Files.exists(mirror, new LinkOption[0])) {
                        bad.add(p);
                        continue;
                    }
                    long size = Files.size(p);
                    if (size != Files.size(mirror) || size % 4096L != 0L) {
                        bad.add(p);
                    }
                } catch (IOException e) {
                    bad.add(p);
                }
            }
        } catch (IOException ignored) {
            // 读不了就不校验
        }
        return bad;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 把一个玩家的数据写进存档点目录：playerdata / advancements / stats。
     * 世界方块、区块一概不存。
     */
    private boolean savePlayerOnly(Path target, ServerPlayer player) {
        if (player == null || this.worldDir == null) {
            return false;
        }
        UUID uuid = player.getUUID();
        boolean any = false;
        try {
            // 直接取内存里的玩家数据（磁盘那份可能是几分钟前的）
            CompoundTag tag = new CompoundTag();
            player.saveWithoutId(tag);
            NbtIo.writeCompressed(tag, target.resolve("playerdata.dat").toFile());
            any = true;
            RegressionMod.LOGGER.info("\u5b58\u6863\u70b9\uff1a\u5df2\u5199\u5165 {} \u7684\u4eba\u7269\u6570\u636e\uff08\u80cc\u5305 {} \u683c\u3001\u7ecf\u9a8c {} \u7ea7\u3001\u4f4d\u7f6e {} {} {}\uff09",
                    player.getName().getString(),
                    tag.getList("Inventory", 10).size(), tag.getInt("XpLevel"),
                    String.format("%.1f", tag.getList("Pos", 6).getDouble(0)),
                    String.format("%.1f", tag.getList("Pos", 6).getDouble(1)),
                    String.format("%.1f", tag.getList("Pos", 6).getDouble(2)));
        } catch (Exception e) {
            RegressionMod.LOGGER.error("\u5b58\u6863\u70b9\uff1a\u5199\u5165\u4eba\u7269\u6570\u636e\u5931\u8d25", (Throwable)e);
        }
        any |= copyIfExists(this.worldDir.resolve("advancements").resolve(uuid + ".json"), target.resolve("advancements.json"));
        any |= copyIfExists(this.worldDir.resolve("stats").resolve(uuid + ".json"), target.resolve("stats.json"));
        return any;
    }

    private static boolean copyIfExists(Path src, Path dst) {
        if (!Files.exists(src, new LinkOption[0])) {
            return false;
        }
        try {
            Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            RegressionMod.LOGGER.warn("\u5b58\u6863\u70b9\uff1a\u590d\u5236 {} \u5931\u8d25\uff1a{}", src.getFileName(), e.getMessage());
            return false;
        }
    }

    private int nextBackupIndex() throws IOException {        int maxIdx = -1;
        if (Files.exists(this.backupRootDir, new LinkOption[0])) {
            try (Stream<Path> dirs = Files.list(this.backupRootDir);){
                for (Path p : dirs.toList()) {
                    String n;
                    if (!Files.isDirectory(p, new LinkOption[0]) || !(n = p.getFileName().toString()).startsWith("backup_")) continue;
                    try {
                        int i = Integer.parseInt(n.substring(7));
                        if (i <= maxIdx) continue;
                        maxIdx = i;
                    }
                    catch (NumberFormatException numberFormatException) {}
                }
            }
        }
        return (maxIdx + 1) % (Integer)ModConfig.MAX_BACKUPS.get();
    }

    private void cleanupOldBackups(int keep) throws IOException {
        if (!Files.exists(this.backupRootDir, new LinkOption[0])) {
            return;
        }
        ArrayList<Path> list = new ArrayList<Path>();
        try (Stream<Path> dirs = Files.list(this.backupRootDir);){
            for (Path p : dirs.toList()) {
                if (!Files.isDirectory(p, new LinkOption[0]) || !p.getFileName().toString().startsWith("backup_")) continue;
                list.add(p);
            }
        }
        list.sort((a, b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()));
        for (int i = keep; i < list.size(); ++i) {
            FileUtils.deleteDirectory((Path)list.get(i));
        }
    }

    public Path findLatestBackup() {
        if (this.backupRootDir == null || !Files.exists(this.backupRootDir, new LinkOption[0])) {
            return null;
        }
        Path latest = null;
        long latestTime = 0L;
        try (Stream<Path> dirs = Files.list(this.backupRootDir);){
            for (Path p : dirs.toList()) {
                long t;
                if (!Files.isDirectory(p, new LinkOption[0]) || !p.getFileName().toString().startsWith("backup_") || (t = p.toFile().lastModified()) <= latestTime) continue;
                latestTime = t;
                latest = p;
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return latest;
    }

    public PlayerSnapshot loadSnapshot(Path backupDir) {
        Path f = backupDir.resolve("snapshot.nbt");
        if (!Files.exists(f, new LinkOption[0])) {
            return null;
        }
        try {
            return PlayerSnapshot.deserialize(NbtIo.readCompressed((File)f.toFile()));
        }
        catch (IOException e) {
            return null;
        }
    }

    public boolean hasBackup() {
        return this.findLatestBackup() != null;
    }

    /**
     * 把世界替换成备份。
     *
     * <p>先把备份完整复制成一个独立目录，再尝试把当前世界改名挪开；
     * Windows 上重命名目录常被文件占用挡住，那就退化成原地覆盖。
     * 任何一步失败，完整的存档点都还在盘上，下次启动会自动收尾。
     */
    public boolean replaceWorldWithBackup(Path backupDir) {
        if (backupDir == null || !Files.exists(backupDir, new LinkOption[0])) {
            RegressionMod.LOGGER.error("\u5907\u4efd\u76ee\u5f55\u4e0d\u5b58\u5728: {}", (Object)backupDir);
            return false;
        }
        if (this.worldDir == null || !Files.exists(this.worldDir.resolve("level.dat"), new LinkOption[0])) {
            RegressionMod.LOGGER.error("\u5b89\u5168\u68c0\u67e5\u5931\u8d25\uff1aworldDir \u4e0d\u5305\u542b level.dat\uff0c\u62d2\u7edd\uff01\u8def\u5f84: {}", (Object)this.worldDir);
            return false;
        }
        String name = this.worldDir.getFileName().toString();
        Path newWorld = this.worldDir.resolveSibling(name + ".regression_new");
        Path oldWorld = this.worldDir.resolveSibling(name + ".pre_regression");
        RegressionMod.LOGGER.info("===== \u66ff\u6362\u4e16\u754c\u6587\u4ef6 =====");
        RegressionMod.LOGGER.info("\u5f53\u524d\u4e16\u754c: {}", (Object)this.worldDir.toAbsolutePath());
        RegressionMod.LOGGER.info("\u5907\u4efd\u6765\u6e90: {}", (Object)backupDir.toAbsolutePath());

        // 第 1 步：把备份完整复制成独立的新世界目录。失败就什么都不动。
        try {
            deleteWithRetry(newWorld, 3);
            FileUtils.copyDirectory(backupDir, newWorld);
            if (!Files.exists(newWorld.resolve("level.dat"), new LinkOption[0])) {
                throw new IOException("\u590d\u5236\u51fa\u6765\u7684\u65b0\u4e16\u754c\u6ca1\u6709 level.dat");
            }
            RegressionMod.LOGGER.info("\u5b58\u6863\u70b9\u5df2\u590d\u5236\u5907\u597d: {}", (Object)newWorld.getFileName());
        } catch (Exception e) {
            RegressionMod.LOGGER.error("\u590d\u5236\u5b58\u6863\u70b9\u5931\u8d25\uff0c\u672c\u6b21\u4e0d\u56de\u6eaf\uff08\u4e16\u754c\u6ca1\u6709\u4efb\u4f55\u6539\u52a8\uff09: {}", (Object)e.getMessage(), (Object)e);
            deleteWithRetry(newWorld, 2);
            return false;
        }

        // 第 2 步：把当前世界挪开（带重试）
        deleteWithRetry(oldWorld, 2);
        if (moveWithRetry(this.worldDir, oldWorld, 4)) {
            try {
                Files.move(newWorld, this.worldDir);
            } catch (Exception e) {
                RegressionMod.LOGGER.error("\u65b0\u4e16\u754c\u653e\u4e0d\u8fdb\u53bb\uff0c\u6b63\u5728\u628a\u539f\u4e16\u754c\u653e\u56de", (Throwable)e);
                if (!Files.exists(this.worldDir.resolve("level.dat"), new LinkOption[0])) {
                    try {
                        Files.move(oldWorld, this.worldDir);
                    } catch (Exception e2) {
                        RegressionMod.LOGGER.error("\u539f\u4e16\u754c\u653e\u56de\u5931\u8d25\uff01\u5b83\u5728 {}\uff0c\u65b0\u4e16\u754c\u5728 {}", (Object)oldWorld, (Object)newWorld);
                    }
                }
                return false;
            }
            deleteWithRetry(oldWorld, 2);
            RegressionMod.LOGGER.info("===== \u4e16\u754c\u66ff\u6362\u5b8c\u6210 =====");
            return true;
        }

        // 第 3 步：改名被占用 → 原地覆盖（session.lock 跳过，它一直被游戏锁着）
        RegressionMod.LOGGER.warn("\u91cd\u547d\u540d\u88ab\u5360\u7528\uff0c\u6539\u4e3a\u539f\u5730\u8986\u76d6\u65b9\u5f0f\u66ff\u6362");
        try {
            overwriteFrom(newWorld, this.worldDir);
            if (!Files.exists(this.worldDir.resolve("level.dat"), new LinkOption[0])) {
                throw new IOException("\u8986\u76d6\u540e\u6ca1\u6709 level.dat");
            }
            deleteWithRetry(newWorld, 2);
            RegressionMod.LOGGER.info("===== \u4e16\u754c\u66ff\u6362\u5b8c\u6210\uff08\u8986\u76d6\u65b9\u5f0f\uff09=====");
            return true;
        } catch (Exception e) {
            RegressionMod.LOGGER.error("\u8986\u76d6\u5931\u8d25\uff1a{}\uff1b\u5b8c\u6574\u7684\u5b58\u6863\u70b9\u4ecd\u5728 {}\uff0c\u4e0b\u6b21\u542f\u52a8\u4f1a\u81ea\u52a8\u5b8c\u6210\u66ff\u6362", (Object)e.getMessage(), (Object)newWorld, (Object)e);
            return false;
        }
    }

    /** 用 src 的内容覆盖 dst：清掉 dst 里的东西（session.lock 除外），再把 src 复制进去。 */
    static void overwriteFrom(Path src, Path dst) throws IOException {
        Files.createDirectories(dst);
        try (Stream<Path> walk = Files.walk(dst)) {
            for (Path p : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                if (p.equals(dst) || "session.lock".equals(p.getFileName().toString())) {
                    continue;
                }
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    RegressionMod.LOGGER.debug("\u65e7\u4e16\u754c\u91cc\u8fd9\u4e2a\u5220\u4e0d\u6389\uff0c\u7565\u8fc7: {}", p.getFileName());
                }
            }
        }
        FileUtils.copyDirectory(src, dst);
    }

    private static boolean moveWithRetry(Path from, Path to, int attempts) {
        for (int i = 0; i < attempts; ++i) {
            try {
                Files.move(from, to);
                return true;
            } catch (Exception e) {
                if (i == attempts - 1) {
                    RegressionMod.LOGGER.warn("\u632a\u5f00 {} \u5931\u8d25\uff08\u5df2\u91cd\u8bd5 {} \u6b21\uff09: {}", from.getFileName(), attempts, e.getClass().getSimpleName());
                } else {
                    RegressionMod.LOGGER.debug("\u632a\u5f00 {} \u7b2c {} \u6b21\u5931\u8d25\uff0c\u7a0d\u540e\u91cd\u8bd5", from.getFileName(), i + 1);
                }
                sleepQuietly(600L);
                System.gc();
            }
        }
        return false;
    }

    private static boolean deleteWithRetry(Path dir, int attempts) {
        if (dir == null) {
            return true;
        }
        for (int i = 0; i < attempts; ++i) {
            if (!Files.exists(dir, new LinkOption[0])) {
                return true;
            }
            try {
                FileUtils.deleteDirectory(dir);
                return true;
            } catch (Exception e) {
                if (i == attempts - 1) {
                    RegressionMod.LOGGER.warn("\u5220\u9664 {} \u5931\u8d25\uff08\u5df2\u91cd\u8bd5 {} \u6b21\uff09: {}", dir.getFileName(), attempts, e.getClass().getSimpleName());
                }
                sleepQuietly(600L);
                System.gc();
            }
        }
        return !Files.exists(dir, new LinkOption[0]);
    }

    /** 上次替换到一半留下的临时世界目录，启动时收尾。 */
    public void recoverInterruptedReplacement() {
        if (this.worldDir == null) {
            return;
        }
        String name = this.worldDir.getFileName().toString();
        Path oldWorld = this.worldDir.resolveSibling(name + ".pre_regression");
        Path newWorld = this.worldDir.resolveSibling(name + ".regression_new");
        boolean hasOld = Files.exists(oldWorld, new LinkOption[0]);
        boolean hasNew = Files.exists(newWorld, new LinkOption[0]);
        if (!hasOld && !hasNew) {
            return;
        }
        boolean worldOk = Files.exists(this.worldDir.resolve("level.dat"), new LinkOption[0]);
        if (worldOk) {
            deleteWithRetry(oldWorld, 2);
            deleteWithRetry(newWorld, 2);
            RegressionMod.LOGGER.info("\u4e0a\u6b21\u56de\u6eaf\u7559\u4e0b\u7684\u4e34\u65f6\u76ee\u5f55\u5df2\u6e05\u7406");
            return;
        }
        if (hasNew && Files.exists(newWorld.resolve("level.dat"), new LinkOption[0])) {
            try {
                overwriteFrom(newWorld, this.worldDir);
                deleteWithRetry(newWorld, 2);
                deleteWithRetry(oldWorld, 2);
                RegressionMod.LOGGER.warn("\u4e0a\u6b21\u56de\u6eaf\u6ca1\u5b8c\u6210\uff0c\u5df2\u5728\u542f\u52a8\u65f6\u628a\u5b58\u6863\u70b9\u653e\u56de\u53bb");
            } catch (Exception e) {
                RegressionMod.LOGGER.error("\u628a {} \u653e\u56de\u5931\u8d25\uff1a{}\uff0c\u8bf7\u624b\u52a8\u5904\u7406", (Object)newWorld, (Object)e.getMessage(), (Object)e);
            }
            return;
        }
        if (hasOld && Files.exists(oldWorld.resolve("level.dat"), new LinkOption[0])) {
            try {
                overwriteFrom(oldWorld, this.worldDir);
                deleteWithRetry(oldWorld, 2);
                deleteWithRetry(newWorld, 2);
                RegressionMod.LOGGER.warn("\u4e0a\u6b21\u56de\u6eaf\u6ca1\u5b8c\u6210\uff0c\u5df2\u628a\u539f\u4e16\u754c\u653e\u56de");
            } catch (Exception e) {
                RegressionMod.LOGGER.error("\u628a {} \u653e\u56de\u5931\u8d25\uff1a{}\uff0c\u8bf7\u624b\u52a8\u5904\u7406", (Object)oldWorld, (Object)e.getMessage(), (Object)e);
            }
        }
    }

    public void logPaths() {
        RegressionMod.LOGGER.info("=== \u8def\u5f84\u8c03\u8bd5\u4fe1\u606f ===");
        RegressionMod.LOGGER.info("worldDir: {}", this.worldDir != null ? this.worldDir.toAbsolutePath() : "null");
        RegressionMod.LOGGER.info("backupRootDir: {}", this.backupRootDir != null ? this.backupRootDir.toAbsolutePath() : "null");
        RegressionMod.LOGGER.info("worldDir \u5b58\u5728: {}", (Object)(this.worldDir != null && Files.exists(this.worldDir, new LinkOption[0]) ? 1 : 0));
        RegressionMod.LOGGER.info("worldDir/level.dat \u5b58\u5728: {}", (Object)(this.worldDir != null && Files.exists(this.worldDir.resolve("level.dat"), new LinkOption[0]) ? 1 : 0));
    }

    public Path getWorldDir() {
        return this.worldDir;
    }

    public Path getBackupRootDir() {
        return this.backupRootDir;
    }
}

