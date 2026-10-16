package com.travel.module2_visa.controller;

import com.travel.module2_visa.domain.ApplicationDocumentEntity;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.service.DocumentStorageService;
import com.travel.module2_visa.service.VisaApplicationService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class FileController {

    private final VisaApplicationService applicationService;
    private final DocumentStorageService storage;

    public FileController(VisaApplicationService applicationService, DocumentStorageService storage) {
        this.applicationService = applicationService;
        this.storage = storage;
    }

    @GetMapping("/files/documents/{id}")
    public ResponseEntity<Resource> document(@PathVariable Long id) {
        ApplicationDocumentEntity doc = applicationService.document(id);
        return serve(storage, doc.getFilePath(), doc.getFileType(), doc.getFileName());
    }

    @GetMapping("/files/applications/{id}/approved-visa")
    public ResponseEntity<Resource> approvedVisa(@PathVariable Long id) {
        VisaApplicationEntity app = applicationService.get(id);
        return serve(storage, app.getApprovedVisaPath(), "application/pdf", "visa-" + id + ".pdf");
    }

    public static ResponseEntity<Resource> serve(DocumentStorageService storage, String storedPath, String contentType, String downloadName) {
        Path path = storage.resolve(storedPath);
        if (path == null || !Files.exists(path)) return ResponseEntity.notFound().build();
        MediaType type;
        try {
            type = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(downloadName == null ? "file" : downloadName).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}
