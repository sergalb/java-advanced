package ru.ifmo.rain.balahnin.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

public class FNVHashFileVisitor extends SimpleFileVisitor<Path> {
    private static final int PRIME = 0x01000193;
    private static final int FIRST = 0x811c9dc5;
    private BufferedWriter writer;

    FNVHashFileVisitor(BufferedWriter writer) {
        this.writer = writer;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        int hash = FIRST;
        try (InputStream inputStream = Files.newInputStream(file)) {
            int symbol;
            byte[] buffer = new byte[1024];
            while ((symbol = inputStream.read(buffer)) >= 0) {
                for (int i = 0; i < symbol; ++i) {
                    hash *= PRIME;
                    hash ^= (buffer[i] & 0xff);
                }
            }
        } catch (IOException | InvalidPathException e) {
            hash = 0;
        } finally {
            try {
                writer.write(String.format("%08x ", hash) + file);
                writer.newLine();
            } catch (IOException e) {
                System.err.println("can't write hash to output file: " + e.getMessage());
            }
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        try {
            writer.write("00000000 " + file);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("can't write hash to output file: " + e.getMessage());
        }
        return CONTINUE;
    }
}
