package ru.ifmo.rain.balahnin.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null) {
            System.err.println("need 2 arguments - input and output files names, now - 0");
            return;
        } else if (args.length != 2) {
            System.err.println("need 2 arguments - input and output files names, now - " + args.length);
            return;
        } else if (args[0] == null | args[1] == null) {
            System.err.println("args is null");
            return;
        }

        Path out;
        try {
            out = Paths.get(args[1]);
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
        } catch (InvalidPathException e) {
            System.err.println("Incorrect path to file: " + args[1]);
            return;
        } catch (IOException e) {
            System.err.println("Error with path create: " + e.getMessage());
            return;
        }

        try (BufferedReader input = new BufferedReader(new FileReader(args[0], Charset.forName("UTF-8")))) {
            try (BufferedWriter writer = Files.newBufferedWriter(out)) {
                String path;
                FileVisitor<Path> visitor = new FNVHashFileVisitor(writer);
                while ((path = input.readLine()) != null) {
                    try {
                        Files.walkFileTree(Paths.get(path), visitor);
                    } catch (InvalidPathException e) {
                        writer.write("00000000 " + path);
                        writer.newLine();
                    }
                }
            } catch (SecurityException e) {
                System.err.println("doesn't enough rights for writing in file" + args[1]);
            } catch (IOException e) {
                System.err.println("output exception " + e.getMessage());
            }
        } catch (NoSuchFileException e) {
            System.err.println("File " + args[0] + "doesn't exist");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}