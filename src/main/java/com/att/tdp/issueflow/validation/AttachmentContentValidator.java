package com.att.tdp.issueflow.validation;

import com.att.tdp.issueflow.Exception.BadRequestException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentContentValidator {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain");

    /**
     * Checks metadata only (presence, declared size, content type). Does not read the request body.
     * Call {@link #readContent(MultipartFile)} only after this passes.
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attachment file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Attachment exceeds maximum size of 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Attachment type not allowed. Allowed: image/png, image/jpeg, application/pdf, text/plain");
        }
    }

    /**
     * Reads file bytes after {@link #validate(MultipartFile)}. Uses a bounded stream so content
     * larger than 10 MB is rejected even if declared size was wrong.
     */
    public byte[] readContent(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return readBounded(input);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read attachment file");
        }
    }

    private byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_FILE_SIZE_BYTES) {
                throw new BadRequestException("Attachment exceeds maximum size of 10 MB");
            }
            output.write(buffer, 0, read);
        }
        if (total == 0) {
            throw new BadRequestException("Attachment file is required");
        }
        return output.toByteArray();
    }
}
