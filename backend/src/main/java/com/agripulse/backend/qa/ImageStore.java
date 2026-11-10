package com.agripulse.backend.qa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stores photos on disk under random names. The type is decided from the file's contents, never from its name. */
@Component
public class ImageStore {
    public record Saved(String name, String type) {}

    private static final long MAX = 5L * 1024 * 1024;
    private static final Pattern NAME = Pattern.compile("^[0-9a-f\\-]{36}\\.(jpg|png|webp)$");
    private final Path dir;

    public ImageStore(@Value("${app.upload-dir:./uploads}") String d) {
        this.dir = Path.of(d).toAbsolutePath().normalize();
        try { Files.createDirectories(dir); }
        catch (IOException e) { throw new IllegalStateException("Cannot create upload folder " + dir, e); }
    }

    public Saved save(MultipartFile f) {
        if (f.getSize() > MAX) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Photo is larger than 5 MB");
        byte[] b;
        try { b = f.getBytes(); }
        catch (IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the photo"); }
        String type = detect(b);
        if (type == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be a JPEG, PNG or WebP image");
        String ext = type.equals("image/jpeg") ? "jpg" : type.equals("image/png") ? "png" : "webp";
        String name = UUID.randomUUID() + "." + ext;
        try { Files.write(dir.resolve(name), b); }
        catch (IOException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the photo"); }
        return new Saved(name, type);
    }

    public byte[] read(String name) {
        if (name == null || !NAME.matcher(name).matches())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        try { return Files.readAllBytes(dir.resolve(name)); }
        catch (IOException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    }

    private static String detect(byte[] b) {
        if (b.length < 12) return null;
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }
}
