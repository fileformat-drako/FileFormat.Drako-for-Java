package dev.fileformat.drako;


import java.nio.Buffer;

@Internal
class IOUtils {
    static void clear(Buffer buffer) {
        buffer.clear();
    }
    static void flip(Buffer buffer) {
        buffer.flip();
    }
    static void limit(Buffer buffer, int newLimit) {
        buffer.limit(newLimit);
    }

    static void position(Buffer buffer, int newPosition) {
        buffer.position(newPosition);
    }

}
