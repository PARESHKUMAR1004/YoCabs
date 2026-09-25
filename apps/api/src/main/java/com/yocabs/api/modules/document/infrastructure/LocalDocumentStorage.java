package com.yocabs.api.modules.document.infrastructure;

import com.yocabs.api.modules.document.application.DocumentStorage;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Filesystem storage for development/single-node deployments. */
@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final Path root;

    public LocalDocumentStorage(@Value("${yocabs.storage.local-path:./data/documents}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, byte[] content) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to store document", exception);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (NoSuchFileException exception) {
            throw new ResourceNotFoundException("Document content not found");
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read document", exception);
        }
    }

    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();

        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        return resolved;
    }
}
