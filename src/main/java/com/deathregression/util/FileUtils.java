/*
 * Decompiled with CFR 0.152.
 */
package com.deathregression.util;

import com.deathregression.DeathRegression;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;

public class FileUtils {
    public static void copyDirectory(final Path sourceDir, final Path targetDir) throws IOException {
        Files.createDirectories(targetDir, new FileAttribute[0]);
        Files.walkFileTree(sourceDir, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relativePath = sourceDir.relativize(dir);
                Path destDir = targetDir.resolve(relativePath);
                if (!Files.exists(destDir, new LinkOption[0])) {
                    Files.createDirectories(destDir, new FileAttribute[0]);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString();
                if (name.endsWith(".lock") || name.equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                Path relativePath = sourceDir.relativize(file);
                Path destFile = targetDir.resolve(relativePath);
                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory, new LinkOption[0])) {
            return;
        }
        Files.walkFileTree(directory, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static long getDirectorySize(Path directory) {
        final long[] size = new long[]{0L};
        try {
            Files.walkFileTree(directory, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size[0] = size[0] + attrs.size();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            DeathRegression.LOGGER.error("\u8ba1\u7b97\u76ee\u5f55\u5927\u5c0f\u65f6\u51fa\u9519: {}", (Object)e.getMessage());
        }
        return size[0];
    }

    public static void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path, new LinkOption[0])) {
            Files.createDirectories(path, new FileAttribute[0]);
            DeathRegression.LOGGER.debug("\u5df2\u521b\u5efa\u76ee\u5f55: {}", (Object)path);
        }
    }

    public static boolean safeDelete(Path path) {
        try {
            return Files.deleteIfExists(path);
        }
        catch (IOException e) {
            DeathRegression.LOGGER.error("\u5220\u9664\u6587\u4ef6\u5931\u8d25: {} - {}", (Object)path, (Object)e.getMessage());
            return false;
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 0x100000L) {
            return String.format("%.2f KB", (double)bytes / 1024.0);
        }
        if (bytes < 0x40000000L) {
            return String.format("%.2f MB", (double)bytes / 1048576.0);
        }
        return String.format("%.2f GB", (double)bytes / 1.073741824E9);
    }
}

