package com.Classroom_ai.Classroom;

import com.Classroom_ai.Classroom.course.Course;
import com.Classroom_ai.Classroom.course.CourseFile;
import com.Classroom_ai.Classroom.material.FileTooLargeException;
import com.Classroom_ai.Classroom.material.Materials;
import com.Classroom_ai.Classroom.material.StoredFile;
import com.Classroom_ai.Classroom.material.StoredFileMissingException;
import com.Classroom_ai.Classroom.material.StoredPicture;
import com.Classroom_ai.Classroom.material.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The {@code material} module through its Java interface, without Spring. */
class MaterialsTest {

    private static final byte[] PDF = "%PDF-1.4 hello".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] GIF = "GIF89a....".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

    @TempDir Path dir;

    private Materials materialsAt(Path uploadDir) {
        return new Materials(uploadDir.toString(), DataSize.ofMegabytes(20), DataSize.ofMegabytes(2));
    }

    // --- P29: the folders exist from startup ---

    @Test
    void startup_createsTheFilesAndPicturesFolders() {
        Path uploadDir = dir.resolve("not-yet-there");

        materialsAt(uploadDir);

        assertTrue(Files.isDirectory(uploadDir.resolve("files")));
        assertTrue(Files.isDirectory(uploadDir.resolve("pictures")));
    }

    // --- P31: the key is relative, so the folder can move ---

    @Test
    void movedUploadFolder_keepsEveryStoredFileReadable() throws Exception {
        Path before = dir.resolve("before");
        StoredFile stored = materialsAt(before).put(new Course(), pdf("a.pdf"));
        String pictureKey = materialsAt(before).putPicture(new MockMultipartFile("p", "me.png", "image/png", PNG));

        Path after = dir.resolve("after");
        Files.move(before, after);
        Materials moved = materialsAt(after);

        CourseFile row = new CourseFile();
        row.setFilePath(stored.key());
        assertArrayEquals(PDF, moved.open(row).getInputStream().readAllBytes());
        assertEquals(MediaType.IMAGE_PNG, moved.openPicture(pictureKey).type());
    }

    @Test
    void key_isRelativeAndNamedByUuid() throws Exception {
        StoredFile stored = materialsAt(dir).put(new Course(), pdf("../../evil.pdf"));

        assertTrue(stored.key().matches("files/[0-9a-f-]{36}\\.pdf"), stored.key());
        assertEquals("evil.pdf", stored.displayName());
    }

    @Test
    void displayName_dropsControlCharactersSoItCannotBreakAHeader() throws Exception {
        StoredFile stored = materialsAt(dir).put(new Course(), pdf("a\r\nSet-Cookie: x=1\u0000.pdf"));

        assertEquals("aSet-Cookie: x=1.pdf", stored.displayName());
    }

    // --- P30: types by magic bytes ---

    @Test
    void put_acceptsOnlyPdfs() {
        Materials materials = materialsAt(dir);

        assertThrows(UnsupportedFileTypeException.class,
                () -> materials.put(new Course(), new MockMultipartFile("file", "a.pdf", "application/pdf", PNG)));
        assertThrows(UnsupportedFileTypeException.class,
                () -> materials.put(new Course(), new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF".getBytes(StandardCharsets.US_ASCII))));
    }

    @Test
    void putPicture_acceptsJpegPngGifWebpAndNamesThemByType() throws Exception {
        Materials materials = materialsAt(dir);

        assertEquals(MediaType.IMAGE_JPEG, materials.openPicture(materials.putPicture(image("a.jpg", JPEG))).type());
        assertEquals(MediaType.IMAGE_PNG, materials.openPicture(materials.putPicture(image("a.png", PNG))).type());
        assertEquals(MediaType.IMAGE_GIF, materials.openPicture(materials.putPicture(image("a.gif", GIF))).type());
        assertEquals("image/webp", materials.openPicture(materials.putPicture(image("a.webp", WEBP))).type().toString());
    }

    @Test
    void putPicture_ignoresTheClientsNameAndExtension() throws Exception {
        Materials materials = materialsAt(dir);

        String key = materials.putPicture(image("../../x.html", PNG));

        assertTrue(key.matches("pictures/[0-9a-f-]{36}\\.png"), key);
    }

    @Test
    void putPicture_refusesSvgHtmlAndPdf() {
        Materials materials = materialsAt(dir);

        for (byte[] body : new byte[][]{
                "<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(StandardCharsets.UTF_8),
                "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8),
                PDF}) {
            assertThrows(UnsupportedFileTypeException.class, () -> materials.putPicture(image("me.png", body)));
        }
    }

    @Test
    void putPicture_overThePictureLimit_isTooLarge() {
        Materials materials = materialsAt(dir);
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        assertThrows(FileTooLargeException.class, () -> materials.putPicture(image("me.png", big)));
    }

    // --- P15, P31: a key never leaves its folder ---

    @Test
    void open_refusesKeysThatLeaveTheFilesFolder() throws Exception {
        Materials materials = materialsAt(dir);
        Path outside = Files.write(dir.resolve("outside.pdf"), PDF);

        for (String key : new String[]{outside.toString(), "files/../outside.pdf", "../outside.pdf", "pictures/x.png", "", null, "files/\0x"}) {
            CourseFile row = new CourseFile();
            row.setFilePath(key);
            assertThrows(StoredFileMissingException.class, () -> materials.open(row), String.valueOf(key));
        }
    }

    @Test
    void openPicture_refusesAKeyInTheFilesFolder() throws Exception {
        Materials materials = materialsAt(dir);
        StoredFile stored = materials.put(new Course(), pdf("a.pdf"));

        assertThrows(StoredFileMissingException.class, () -> materials.openPicture(stored.key()));
    }

    // --- S6: deleting a course file removes its stored PDF, and only that ---

    @Test
    void delete_removesTheStoredPdf() throws Exception {
        Materials materials = materialsAt(dir);
        StoredFile stored = materials.put(new Course(), pdf("a.pdf"));
        CourseFile row = new CourseFile();
        row.setFilePath(stored.key());

        materials.delete(row);

        assertTrue(Files.notExists(dir.resolve(stored.key())));
    }

    @Test
    void delete_ofAMissingFile_isQuiet() {
        Materials materials = materialsAt(dir);
        CourseFile row = new CourseFile();
        row.setFilePath("files/00000000-0000-0000-0000-000000000000.pdf");

        materials.delete(row);
    }

    @Test
    void delete_neverTouchesAFileOutsideTheFilesFolder() throws Exception {
        Materials materials = materialsAt(dir);
        Path outside = Files.write(dir.resolve("outside.pdf"), PDF);
        Path picture = Files.write(dir.resolve("pictures").resolve("x.png"), PNG);

        for (String key : new String[]{outside.toString(), "files/../outside.pdf", "../outside.pdf", "pictures/x.png", "", null, "files/\0x"}) {
            CourseFile row = new CourseFile();
            row.setFilePath(key);
            materials.delete(row);
        }

        assertTrue(Files.exists(outside));
        assertTrue(Files.exists(picture));
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }

    private static MockMultipartFile image(String name, byte[] body) {
        return new MockMultipartFile("profilePicture", name, "image/png", body);
    }
}
