/*
 * Copyright 2014-2022 Rudy De Busscher (https://www.atbash.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.atbash.util.codec;
/*
 * Taken with small modifications
 * <p>
 * Copyright 2009 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("squid:S1226")
public class Base32Codec {
    private static final String SEPARATOR = "-";

    private final char[] digits;
    private final int mask;
    private final int shift;
    private final Map<Character, Integer> charMap;


    private Base32Codec(String alphabet) {
        // 32 alpha-numeric characters.
        digits = alphabet.toCharArray();
        mask = digits.length - 1;
        shift = Integer.numberOfTrailingZeros(digits.length);
        charMap = new HashMap<>();
        for (int i = 0; i < digits.length; i++) {
            charMap.put(digits[i], i);
        }
    }

    public static byte[] decode(String encoded) {
        return getInstance().decodeInternal(encoded);
    }

    protected byte[] decodeInternal(String encoded) {
        // Remove whitespace and separators
        encoded = encoded.trim().replace(SEPARATOR, "").replace(" ", "");

        // Remove padding. Note: the padding is used as hint to determine how many
        // bits to decode from the last incomplete chunk (which is commented out
        // below, so this may have been wrong to start with).
        encoded = encoded.replaceFirst("[=]*$", "");

        // Canonicalize to all upper case
        encoded = encoded.toUpperCase(Locale.US);
        if (encoded.length() == 0) {
            return new byte[0];
        }
        int encodedLength = encoded.length();
        int outLength = encodedLength * shift / 8;
        byte[] result = new byte[outLength];
        int buffer = 0;
        int next = 0;
        int bitsLeft = 0;
        for (char c : encoded.toCharArray()) {
            if (!charMap.containsKey(c)) {
                throw new CodecException("Illegal character: " + c);
            }
            buffer <<= shift;
            buffer |= charMap.get(c) & mask;
            bitsLeft += shift;
            if (bitsLeft >= 8) {
                result[next++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        // We'll ignore leftover bits for now.
        return result;
    }

    public static String encodeToString(byte[] data) {
        return getInstance().encodeInternal(data);
    }

    protected String encodeInternal(byte[] data) {
        if (data.length == 0) {
            return "";
        }

        // SHIFT is the number of bits per output character, so the length of the
        // output is the length of the input multiplied by 8/SHIFT, rounded up.
        if (data.length >= (1 << 28)) {
            // The computation below will fail, so don't do it.
            throw new IllegalArgumentException();
        }

        int outputLength = (data.length * 8 + shift - 1) / shift;
        StringBuilder result = new StringBuilder(outputLength);

        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < shift) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= (data[next++] & 0xff);
                    bitsLeft += 8;
                } else {
                    int pad = shift - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = mask & (buffer >> (bitsLeft - shift));
            bitsLeft -= shift;
            result.append(digits[index]);
        }
        return result.toString();
    }

    private static final Base32Codec INSTANCE =
            new Base32Codec("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"); // RFC 4648/3548

    private static Base32Codec getInstance() {
        return INSTANCE;
    }

}
