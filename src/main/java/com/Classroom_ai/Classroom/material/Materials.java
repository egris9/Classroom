package com.Classroom_ai.Classroom.material;

import com.Classroom_ai.Classroom.course.Course;
import com.Classroom_ai.Classroom.course.CourseFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

/**
 * The only code that touches the upload folder. A course file is stored as {@code files/<uuid>.pdf} and a
 * picture as {@code pictures/<uuid>.<ext>}, both under {@code upload.dir}. The database keeps that relative
 * key, so the folder can move without a rewrite. The client's file name is never used on disk.
 */
@Component
public class Materials {

    private static final Logger log = LoggerFactory.getLogger(Materials.class);
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final String FALLBACK_NAME = "file.pdf";
    private static final int MAX_DISPLAY_NAME = 255;

    private final Path root;
    private final Path filesDir;
    private final Path picturesDir;
    private final long maxFileBytes;
    private final long maxPictureBytes;

    public Materials(@Value("${upload.dir}") String uploadDir,
                     @Value("${upload.max-size}") DataSize maxFileSize,
                     @Value("${upload.max-picture-size}") DataSize maxPictureSize) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.filesDir = root.resolve("files");
        this.picturesDir = root.resolve("pictures");
        this.maxFileBytes = maxFileSize.toBytes();
        this.maxPictureBytes = maxPictureSize.toBytes();
        try {
            Files.createDirectories(filesDir);
            Files.createDirectories(picturesDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the upload folders under " + root, e);
        }
    }

    /** Stores a PDF for the course. 413 when over the size limit, 415 when it does not start with {@code %PDF-}. */
    public StoredFile put(Course course, MultipartFile upload) throws IOException {
        if (upload.getSize() > maxFileBytes) {
            throw new FileTooLargeException("The file is larger than " + DataSize.ofBytes(maxFileBytes).toMegabytes() + "MB.");
        }
        requirePdf(upload);
        String key = "files/" + UUID.randomUUID() + ".pdf";
        write(upload, key);
        log.debug("Stored {} for course {}", key, course.getId());
        return new StoredFile(key, displayName(upload.getOriginalFilename()));
    }

    /** Throws 415 unless the upload starts with {@code %PDF-}, whatever its name or declared type says. */
    public void requirePdf(MultipartFile upload) throws IOException {
        if (!Arrays.equals(head(upload, PDF_MAGIC.length), PDF_MAGIC)) {
            throw new UnsupportedFileTypeException("Only PDF files are accepted.");
        }
    }

    /** The stored PDF of a course file. */
    public Resource open(CourseFile file) {
        return resolve(filesDir, file.getFilePath());
    }

    /** Removes the stored PDF of a course file. A file that is already gone, or a key outside the files folder, is left alone. */
    public void delete(CourseFile file) {
        try {
            Files.deleteIfExists(resolve(filesDir, file.getFilePath()).getFile().toPath());
        } catch (StoredFileMissingException e) {
            log.debug("Nothing to delete for course file {}", file.getId());
        } catch (IOException e) {
            log.warn("Could not delete the stored PDF of course file {}", file.getId(), e);
        }
    }

    /** Stores a profile picture and returns its key. 413 over the picture limit, 415 unless JPEG, PNG, GIF or WebP. */
    public String putPicture(MultipartFile upload) throws IOException {
        if (upload.getSize() > maxPictureBytes) {
            throw new FileTooLargeException("The picture is larger than " + DataSize.ofBytes(maxPictureBytes).toMegabytes() + "MB.");
        }
        ImageType type = ImageType.sniff(head(upload, 12));
        if (type == null) {
            throw new UnsupportedFileTypeException("Pictures must be JPEG, PNG, GIF or WebP.");
        }
        String key = "pictures/" + UUID.randomUUID() + "." + type.extension();
        write(upload, key);
        return key;
    }

    /** The stored picture, with the content type its extension names. */
    public StoredPicture openPicture(String key) {
        Resource resource = resolve(picturesDir, key);
        ImageType type = ImageType.ofFileName(resource.getFilename());
        if (type == null) {
            throw new StoredFileMissingException();
        }
        return new StoredPicture(resource, type.mediaType());
    }

    /** Removes a stored picture. A picture that is already gone, or a key outside the pictures folder, is left alone. */
    public void deletePicture(String key) {
        try {
            Files.deleteIfExists(resolve(picturesDir, key).getFile().toPath());
        } catch (StoredFileMissingException e) {
            log.debug("Nothing to delete for picture key");
        } catch (IOException e) {
            log.warn("Could not delete a stored picture", e);
        }
    }

    private void write(MultipartFile upload, String key) throws IOException {
        Path target = root.resolve(key);
        Files.createDirectories(target.getParent());
        try (InputStream in = upload.getInputStream()) {
            Files.copy(in, target);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    private static byte[] head(MultipartFile upload, int length) throws IOException {
        try (InputStream in = upload.getInputStream()) {
            return in.readNBytes(length);
        }
    }

    /** The file a key names, if it is a regular file inside {@code dir}. A key that climbs out of it names nothing. */
    private Resource resolve(Path dir, String key) {
        if (key == null || key.isBlank()) {
            throw new StoredFileMissingException();
        }
        try {
            Path path = root.resolve(key).normalize();
            if (!path.startsWith(dir) || !Files.isRegularFile(path)) {
                throw new StoredFileMissingException();
            }
            return new FileSystemResource(path);
        } catch (InvalidPathException e) {
            throw new StoredFileMissingException();
        }
    }

    /** The last segment of the client's name, for showing to users only. */
    static String displayName(String original) {
        if (original == null) {
            return FALLBACK_NAME;
        }
        String name = original.replaceAll("\\p{Cntrl}", "").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();
        if (name.isEmpty() || name.equals("..")) {
            return FALLBACK_NAME;
        }
        return name.length() > MAX_DISPLAY_NAME ? name.substring(name.length() - MAX_DISPLAY_NAME) : name;
    }

    /** The image formats a picture may have, told apart by their first bytes. */
    private enum ImageType {
        JPEG("jpg", MediaType.IMAGE_JPEG),
        PNG("png", MediaType.IMAGE_PNG),
        GIF("gif", MediaType.IMAGE_GIF),
        WEBP("webp", MediaType.parseMediaType("image/webp"));

        private final String extension;
        private final MediaType mediaType;

        ImageType(String extension, MediaType mediaType) {
            this.extension = extension;
            this.mediaType = mediaType;
        }

        String extension() {
            return extension;
        }

        MediaType mediaType() {
            return mediaType;
        }

        static ImageType sniff(byte[] h) {
            if (startsWith(h, 0xFF, 0xD8, 0xFF)) {
                return JPEG;
            }
            if (startsWith(h, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
                return PNG;
            }
            if (startsWith(h, 'G', 'I', 'F', '8') && h.length >= 6 && (h[4] == '7' || h[4] == '9') && h[5] == 'a') {
                return GIF;
            }
            if (startsWith(h, 'R', 'I', 'F', 'F') && h.length >= 12
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
                return WEBP;
            }
            return null;
        }

        static ImageType ofFileName(String name) {
            if (name == null) {
                return null;
            }
            for (ImageType t : values()) {
                if (name.endsWith("." + t.extension)) {
                    return t;
                }
            }
            return null;
        }

        private static boolean startsWith(byte[] h, int... prefix) {
            if (h.length < prefix.length) {
                return false;
            }
            for (int i = 0; i < prefix.length; i++) {
                if ((h[i] & 0xFF) != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
