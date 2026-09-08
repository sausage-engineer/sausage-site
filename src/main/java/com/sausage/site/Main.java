package com.sausage.site;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];
        Path projectDir = args.length > 1 ? Paths.get(args[1]).toAbsolutePath().normalize() : Paths.get(".").toAbsolutePath().normalize();

        switch (command) {
            case "build" -> SiteBuilder.build(projectDir);
            case "preview" -> SiteBuilder.preview(projectDir);
            case "help", "--help", "-h" -> printUsage();
            default -> {
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar sausage-site.jar build [project-dir]");
        System.out.println("  java -jar sausage-site.jar preview [project-dir]");
    }
}
