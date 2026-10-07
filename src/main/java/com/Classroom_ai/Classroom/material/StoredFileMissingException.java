package com.Classroom_ai.Classroom.material;

/** The key names no file in the folder it belongs to. */
public class StoredFileMissingException extends RuntimeException {
    public StoredFileMissingException() {
        super("The stored file is missing.");
    }
}
