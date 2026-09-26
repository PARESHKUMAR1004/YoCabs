package com.yocabs.api.modules.document.application;

/**
 * Port for private object storage. Implementations must never expose
 * content by a guessable/public location (S3 adapters should use private
 * buckets and server-side access only).
 */
public interface DocumentStorage {

    void store(String key, byte[] content);

    byte[] load(String key);

    /** Removes the content; a key that is already gone is not an error. */
    void delete(String key);
}
