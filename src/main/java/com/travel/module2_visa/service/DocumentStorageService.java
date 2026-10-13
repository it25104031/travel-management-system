package com.travel.module2_visa.service;

import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.RequiredDocumentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded files on disk and keeps disk and database in step:
 * a file written inside a transaction that later rolls back is removed again, and a
 * file deleted inside a transaction is only removed from disk after the commit succeeds.
 */
@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private static final Map<String, List<String>> MIME_BY_EXTENSION = Map.of(
            "jpg", List.of("image/jpeg", "image/jpg", "image/pjpeg"),
            "jpeg", List.of("image/jpeg", "image/jpg", "image/pjpeg"),
            "png", List.of("image/png"),
            "pdf", List.of("application/pdf"));

    private static final List<String> DEFAULT_FORMATS = List.of("jpg", "jpeg", "png", "pdf");
    private static final int DEFAULT_MAX_MB = 5;

    public record StoredFile(String path, String originalName, String contentType, long size) { }

    private final Path root;

    public DocumentStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Checks format, size and (for images) minimum dimensions. A null rule applies the default limits. */
    public void validate(MultipartFile file, RequiredDocumentEntity rule, String label) {
        List<String> errors = new ArrayList<>();
        String name = label != null ? label : (rule != null ? rule.getLabel() : "File");
        if (file == null || file.isEmpty()) {
            throw new ValidationException(name + ": no file was selected.");
        }
        List<String> allowed = rule != null && !rule.getAllowedFormatList().isEmpty() ? rule.getAllowedFormatList() : DEFAULT_FORMATS;
        int maxMb = rule != null && rule.getMaxSizeMb() != null ? rule.getMaxSizeMb() : DEFAULT_MAX_MB;

        String ext = extension(file.getOriginalFilename());
        if (!allowed.contains(ext)) {
            errors.add(name + ": format ." + ext + " is not allowed (allowed: " + String.join(", ", allowed) + ").");
        } else {
            List<String> mimes = MIME_BY_EXTENSION.get(ext);
            if (mimes != null && file.getContentType() != null && !mimes.contains(file.getContentType().toLowerCase(Locale.ROOT))) {
                errors.add(name + ": the file content type (" + file.getContentType() + ") does not match its extension.");
            }
        }
        if (file.getSize() > maxMb * 1024L * 1024L) {
            errors.add(name + ": file is " + String.format(Locale.ROOT, "%.1f", file.getSize() / 1048576.0) + " MB; the maximum is " + maxMb + " MB.");
        }
        if (errors.isEmpty() && rule != null && (rule.getMinWidthPx() != null || rule.getMinHeightPx() != null)
                && List.of("jpg", "jpeg", "png").contains(ext)) {
            try (InputStream in = file.getInputStream()) {
                BufferedImage image = ImageIO.read(in);
                if (image == null) {
                    errors.add(name + ": the image could not be read.");
                } else {
                    int minW = rule.getMinWidthPx() == null ? 0 : rule.getMinWidthPx();
                    int minH = rule.getMinHeightPx() == null ? 0 : rule.getMinHeightPx();
                    if (image.getWidth() < minW || image.getHeight() < minH) {
                        errors.add(name + ": image is " + image.getWidth() + "x" + image.getHeight()
                                + " px; minimum is " + minW + "x" + minH + " px.");
                    }
                }
            } catch (IOException e) {
                errors.add(name + ": the image could not be read.");
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    public StoredFile store(MultipartFile file, String subdir) {
        String ext = extension(file.getOriginalFilename());
        // Never use the client's file name on disk; it may contain path segments
        String storedName = System.currentTimeMillis() + "_" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path dir = root.resolve(subdir).normalize();
        Path target = dir.resolve(storedName);
        if (!target.startsWith(root)) throw new ValidationException("Invalid upload location.");
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the uploaded file", e);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) deleteQuietly(target);
                }
            });
        }
        String relative = Paths.get("").toAbsolutePath().relativize(target).toString().replace('\\', '/');
        String original = file.getOriginalFilename() == null ? storedName : Paths.get(file.getOriginalFilename()).getFileName().toString();
        return new StoredFile(relative, original, file.getContentType(), file.getSize());
    }

    /** Removes the file once the surrounding transaction commits (immediately when there is none). */
    public void deleteAfterCommit(String storedPath) {
        if (storedPath == null) return;
        Path path = resolve(storedPath);
        if (path == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { deleteQuietly(path); }
            });
        } else {
            deleteQuietly(path);
        }
    }

    /** Resolves a stored path, refusing anything outside the upload root. */
    public Path resolve(String storedPath) {
        if (storedPath == null) return null;
        Path path = Paths.get(storedPath).toAbsolutePath().normalize();
        return path.startsWith(root) ? path : null;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete {}: {}", path, e.getMessage());
        }
    }

    static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
