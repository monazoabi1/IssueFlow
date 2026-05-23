package com.att.tdp.issueflow.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.Exception.BadRequestException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class AttachmentContentValidatorTest {

    private final AttachmentContentValidator validator = new AttachmentContentValidator();

    /** Goal: Allowed PNG uploads pass validation without error. */
    @Test
    void validate_allowedPng_succeeds() {
        MockMultipartFile file = new MockMultipartFile("file", "shot.png", "image/png", new byte[] {1, 2, 3});

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    /** Goal: Disallowed MIME types are rejected with a clear error. */
    @Test
    void validate_disallowedType_rejects() {
        MockMultipartFile file =
                new MockMultipartFile("file", "script.exe", "application/octet-stream", new byte[] {1});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not allowed");
    }

    /** Goal: Empty attachment files are rejected as required content. */
    @Test
    void validate_emptyFile_rejects() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required");
    }

    /** Goal: Files exceeding the 10 MB limit are rejected. */
    @Test
    void validate_fileTooLarge_rejects() {
        byte[] payload = new byte[(int) AttachmentContentValidator.MAX_FILE_SIZE_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", payload);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("10 MB");
    }

    /** Goal: readContent returns the validated file bytes after validate succeeds. */
    @Test
    void readContent_afterValidate_returnsBytes() {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

        validator.validate(file);

        assertThat(validator.readContent(file)).containsExactly("hello".getBytes());
    }

    /** Goal: readContent enforces size limit from stream even when declared size is small. */
    @Test
    void readContent_streamExceedsLimit_rejectsEvenWhenDeclaredSizeSmall() {
        MultipartFile file = new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return "big.png";
            }

            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 4;
            }

            @Override
            public byte[] getBytes() {
                throw new UnsupportedOperationException("Should use getInputStream");
            }

            @Override
            public InputStream getInputStream() {
                byte[] oversized = new byte[(int) AttachmentContentValidator.MAX_FILE_SIZE_BYTES + 1];
                return new ByteArrayInputStream(oversized);
            }

            @Override
            public void transferTo(java.io.File dest) {
                throw new UnsupportedOperationException();
            }
        };

        validator.validate(file);
        assertThatThrownBy(() -> validator.readContent(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("10 MB");
    }
}
