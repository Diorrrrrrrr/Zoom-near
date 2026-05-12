package kr.zoomnear.domain.profile;

import java.security.SecureRandom;

/// 6자리 고유 코드 채번기. Crockford Base32 (0-9, A-Z 중 I/L/O/U 제외).
/// 32^6 ≈ 1.07B 공간이라 100만 사용자에서도 충돌 확률 < 0.1%.
public final class UniqueCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UniqueCodeGenerator() {}

    public static String generate() {
        char[] code = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            code[i] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        }
        return new String(code);
    }
}
