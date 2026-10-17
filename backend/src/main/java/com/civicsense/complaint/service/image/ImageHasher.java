package com.civicsense.complaint.service.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Lightweight, zero-dependency image hashing utility.
 *
 * <p>Provides:
 * <ul>
 *   <li><b>SHA-256</b>: cryptographic hash for exact byte-identical match detection.</li>
 *   <li><b>dHash (Difference Hash)</b>: 64-bit perceptual hash for detecting visually
 *       identical or re-compressed images across platforms (e.g. WhatsApp re-encodings).</li>
 * </ul>
 */
public final class ImageHasher {

    private ImageHasher() {}

    /**
     * Computes the SHA-256 hexadecimal string for the given raw bytes.
     */
    public static String computeSha256(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    /**
     * Computes a 64-bit Difference Hash (dHash) represented as a 16-character hex string.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Resize image to 9 columns x 8 rows in grayscale.</li>
     *   <li>Compute 64 horizontal gradient bits: bit = 1 if pixel(x, y) > pixel(x + 1, y), else 0.</li>
     *   <li>Pack into a 64-bit unsigned integer and format as hex.</li>
     * </ol>
     */
    public static String computeDHash(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage original = ImageIO.read(bais);
            if (original == null) {
                return null;
            }

            BufferedImage small = new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = small.createGraphics();
            g.drawImage(original, 0, 0, 9, 8, null);
            g.dispose();

            long hash = 0L;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int left = small.getRaster().getSample(x, y, 0);
                    int right = small.getRaster().getSample(x + 1, y, 0);
                    if (left > right) {
                        hash |= (1L << (y * 8 + x));
                    }
                }
            }

            return String.format("%016x", hash);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calculates the Hamming distance (number of bit differences) between two 16-hex dHash strings.
     *
     * @return number of differing bits (0 to 64), or -1 if either hash is invalid.
     */
    public static int calculateHammingDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null || hash1.length() != 16 || hash2.length() != 16) {
            return -1;
        }

        try {
            long h1 = Long.parseUnsignedLong(hash1, 16);
            long h2 = Long.parseUnsignedLong(hash2, 16);
            return Long.bitCount(h1 ^ h2);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

