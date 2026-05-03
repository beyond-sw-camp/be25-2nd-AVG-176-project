package com.example.team3Project.support.fastapi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "fastapi.autostart.enabled", havingValue = "true", matchIfMissing = true)
public class FastApiServerLifecycle implements SmartLifecycle {

    private final Path workingDir;
    private final String host;
    private final int port;
    private final String pythonExecutable;
    private final String appModule;
    private final String scriptName;

    private volatile boolean running;
    private Process process;
    private Thread logThread;

    public FastApiServerLifecycle(
            @Value("${fastapi.autostart.working-dir:../team3ProjectSourcing/Python-Backend}") String workingDir,
            @Value("${fastapi.autostart.host:localhost}") String host,
            @Value("${fastapi.autostart.port:8000}") int port,
            @Value("${fastapi.autostart.python:}") String pythonExecutable,
            @Value("${fastapi.autostart.app-module:image_translate:app}") String appModule,
            @Value("${fastapi.autostart.script:image_translate.py}") String scriptName) {
        this.workingDir = Path.of(workingDir).toAbsolutePath().normalize();
        this.host = host;
        this.port = port;
        this.pythonExecutable = pythonExecutable;
        this.appModule = appModule;
        this.scriptName = scriptName;
    }

    @Override
    public void start() {
        if (running || isPortOpen()) {
            running = true;
            log.info("FastAPI server already available at {}:{}", host, port);
            return;
        }

        if (!Files.isDirectory(workingDir)) {
            log.warn("FastAPI working directory does not exist: {}", workingDir);
            return;
        }

        List<String> command = buildCommand();
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true);
            process = builder.start();
            startLogThread(process);
            running = true;
            log.info("Started FastAPI server: command={} dir={}", command, workingDir);
        } catch (IOException e) {
            log.warn("Failed to start FastAPI server. command={} dir={} error={}", command, workingDir, e.getMessage());
        }
    }

    @Override
    public void stop() {
        running = false;
        if (process == null || !process.isAlive()) {
            return;
        }

        process.destroy();
        try {
            if (!process.waitFor(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        log.info("Stopped FastAPI server process");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    private List<String> buildCommand() {
        String python = resolvePythonExecutable();
        return List.of(python, "-m", "uvicorn", appModule, "--host", "0.0.0.0", "--port", String.valueOf(port));
    }

    private String resolvePythonExecutable() {
        if (pythonExecutable != null && !pythonExecutable.isBlank()) {
            return pythonExecutable;
        }

        Path venvPython = workingDir.resolve(".venv").resolve("bin").resolve("python");
        if (Files.isExecutable(venvPython)) {
            return venvPython.toString();
        }

        Path script = workingDir.resolve(scriptName);
        if (Files.isRegularFile(script)) {
            return "python3";
        }

        return "python3";
    }

    private boolean isPortOpen() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 300);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void startLogThread(Process process) {
        logThread = new Thread(() -> {
            try (var reader = process.inputReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[fastapi] {}", line);
                }
            } catch (IOException e) {
                log.debug("FastAPI log reader stopped: {}", e.getMessage());
            }
        }, "fastapi-log-reader");
        logThread.setDaemon(true);
        logThread.start();
    }
}
