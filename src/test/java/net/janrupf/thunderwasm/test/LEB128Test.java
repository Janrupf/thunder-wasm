package net.janrupf.thunderwasm.test;

import net.janrupf.thunderwasm.module.encoding.LEB128Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LEB128Test {
    private static class TestPair {
        private final String textValue;
        private final byte[] bytes;

        public TestPair(String textValue, byte[] bytes) {
            this.textValue = textValue;
            this.bytes = bytes;
        }

        public String getTextValue() {
            return textValue;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }

    private static List<TestPair> loadTestData(String name) throws IOException {
        List<TestPair> out = new ArrayList<>();

        try (
                InputStream stream = LEB128Test.class.getResourceAsStream(name);
        ) {
            if (stream == null) {
                throw new IOException("Resource not found: " + name);
            }

            try (
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ");
                    if (parts.length != 2) {
                        throw new IOException("Invalid line: " + line);
                    }

                    String textValue = parts[0];
                    byte[] bytes = new byte[parts[1].length() / 2];

                    for (int i = 0; i < bytes.length; i++) {
                        bytes[i] = (byte) Integer.parseInt(parts[1].substring(i * 2, i * 2 + 2), 16);
                    }

                    out.add(new TestPair(textValue, bytes));
                }
            }
        }

        return out;
    }

    @Test
    public void testDecodeSigned() throws Exception {
        List<TestPair> testData = loadTestData("/leb128/signed.txt");

        for (TestPair pair : testData) {
            try (
                    InputStream stream = new ByteArrayInputStream(pair.getBytes());
            ) {
                long value = LEB128Value.readS64(stream);
                long expected = Long.parseLong(pair.getTextValue());

                Assertions.assertEquals(expected, value);
            }
        }
    }

    @Test
    public void testDecodeUnsigned() throws Exception {
        List<TestPair> testData = loadTestData("/leb128/unsigned.txt");

        for (TestPair pair : testData) {
            try (
                    InputStream stream = new ByteArrayInputStream(pair.getBytes());
            ) {
                long value = LEB128Value.readU64(stream);
                long expected = Long.parseUnsignedLong(pair.getTextValue());

                Assertions.assertEquals(expected, value);
            }
        }
    }
}
