package io.flagship.core.strategy;

import java.nio.charset.StandardCharsets;

/**
 * Standard 32-bit MurmurHash3 algorithm implementation (Austin Appleby).
 * Seed is default 0. Guarantees 100% deterministic, cross-JVM consistent hashes.
 */
public final class MurmurHash3 {

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    private MurmurHash3() {}

    /**
     * Compute 32-bit MurmurHash3 for a UTF-8 string with seed = 0.
     *
     * @param input the input string
     * @return 32-bit integer hash
     */
    public static int hash32(String input) {
        if (input == null) {
            return 0;
        }
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        return hash32(bytes, 0, bytes.length, 0);
    }

    /**
     * Compute 32-bit MurmurHash3 for a byte array.
     *
     * @param data bytes
     * @param offset starting offset
     * @param length length of bytes
     * @param seed initial seed
     * @return 32-bit integer hash
     */
    public static int hash32(byte[] data, int offset, int length, int seed) {
        int h1 = seed;
        int nblocks = length >> 2;

        for (int i = 0; i < nblocks; i++) {
            int index = offset + (i << 2);
            int k1 = (data[index] & 0xff)
                    | ((data[index + 1] & 0xff) << 8)
                    | ((data[index + 2] & 0xff) << 16)
                    | ((data[index + 3] & 0xff) << 24);

            k1 *= C1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= C2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        int tailIndex = offset + (nblocks << 2);
        int k1 = 0;
        switch (length & 3) {
            case 3:
                k1 ^= (data[tailIndex + 2] & 0xff) << 16;
            case 2:
                k1 ^= (data[tailIndex + 1] & 0xff) << 8;
            case 1:
                k1 ^= (data[tailIndex] & 0xff);
                k1 *= C1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= C2;
                h1 ^= k1;
            default:
                break;
        }

        // Finalization
        h1 ^= length;
        h1 ^= (h1 >>> 16);
        h1 *= 0x85ebca6b;
        h1 ^= (h1 >>> 13);
        h1 *= 0xc2b2ae35;
        h1 ^= (h1 >>> 16);

        return h1;
    }
}
