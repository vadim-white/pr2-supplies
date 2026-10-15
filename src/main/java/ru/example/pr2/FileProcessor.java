package ru.example.pr2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileProcessor {
    static final Set<String> EXT = Set.of("csv", "json", "txt");
    static final Pattern STATUS = Pattern.compile("\"status\"\\s*:\\s*\"(\\w+)\"|status[:=]\\s*(\\w+)", Pattern.CASE_INSENSITIVE);

    public static void listDir(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> files = s.sorted().toList();
            if (files.isEmpty()) {
                System.out.println("  (пусто)");
                return;
            }
            for (Path p : files) {
                BasicFileAttributes at = Files.readAttributes(p, BasicFileAttributes.class);
                System.out.printf("  %-20s %8d bytes  %s%n", p.getFileName(), at.size(), at.lastModifiedTime());
            }
        }
    }

    public static List<Path> findInputFiles(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> EXT.contains(ext(p)))
                    .sorted()
                    .toList();
        }
    }

    public static void process(Path file, Path processedDir) throws IOException, NoSuchAlgorithmException {
        byte[] data = readViaChannel(file);
        String text = new String(data, StandardCharsets.UTF_8);
        String status = extractStatus(text);
        int lineCount = (int) text.lines().count();
        String hash = sha256Hex(data);

        Path dst = processedDir.resolve(file.getFileName());
        Files.move(file, dst, StandardCopyOption.REPLACE_EXISTING);

        System.out.println(file.getFileName());
        System.out.println("Size: " + data.length + " bytes");
        System.out.println("SHA-256: " + hash);
        if (status != null) System.out.println("Status (из содержимого): " + status);
        System.out.println("Строк: " + lineCount);
        System.out.println("Status: processed");
        System.out.println();
    }

    static byte[] readViaChannel(Path file) throws IOException {
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate((int) ch.size());
            while (buf.hasRemaining() && ch.read(buf) != -1) { }
            buf.flip();
            byte[] out = new byte[buf.remaining()];
            buf.get(out);
            return out;
        }
    }

    static String extractStatus(String text) {
        Matcher m = STATUS.matcher(text);
        if (m.find()) return m.group(1) != null ? m.group(1) : m.group(2);
        return null;
    }

    static String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder(h.length * 2);
        for (byte b : h) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    static String ext(Path p) {
        String n = p.getFileName().toString();
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1).toLowerCase();
    }
}
