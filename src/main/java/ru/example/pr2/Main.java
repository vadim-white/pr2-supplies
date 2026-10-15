package ru.example.pr2;

import java.nio.file.*;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Path base = Path.of("data");
        Path incoming = base.resolve("incoming");
        Path processed = base.resolve("processed");

        for (Path d : List.of(incoming, processed)) {
            if (!Files.exists(d)) {
                System.out.println("Каталог " + d + " не найден, создаю");
                Files.createDirectories(d);
            }
        }

        System.out.println("Содержимое " + incoming + ":");
        FileProcessor.listDir(incoming);
        System.out.println();

        System.out.println("Обработка уже лежащих во входном каталоге файлов:");
        for (Path f : FileProcessor.findInputFiles(incoming)) {
            FileProcessor.process(f, processed);
        }

        WatchService ws = FileSystems.getDefault().newWatchService();
        incoming.register(ws, ENTRY_CREATE);
        System.out.println("Слежу за " + incoming.toAbsolutePath() + " (Ctrl+C для выхода)");

        while (true) {
            WatchKey key = ws.take();
            for (WatchEvent<?> ev : key.pollEvents()) {
                if (ev.kind() == OVERFLOW) continue;
                Path name = (Path) ev.context();
                Path file = incoming.resolve(name);
                System.out.println("Обнаружен новый файл: " + name);
                Thread.sleep(100);
                if (Files.isRegularFile(file) && FileProcessor.EXT.contains(FileProcessor.ext(file))) {
                    FileProcessor.process(file, processed);
                }
            }
            key.reset();
        }
    }
}
