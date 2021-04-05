package sym.ads.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by vbondarenko on 26.05.2020.
 */

class UtilsTest {

    @Test
    void toAmount() {
        assertEquals("0.000001", Utils.toAmount(1L, 6, '.'));
        assertEquals("1.000000", Utils.toAmount(1_000_000L, 6, '.'));
        assertEquals("8999999999.000000", Utils.toAmount(8_999_999_999_000_000L, 6, '.'));
        assertEquals("8999999999.999999", Utils.toAmount(8_999_999_999_999_999L, 6, '.'));

        assertEquals("0.001", Utils.toAmount(1L, 3, '.'));
        assertEquals("1.000", Utils.toAmount(1_000L, 3, '.'));
        assertEquals("8999999999000.000", Utils.toAmount(8_999_999_999_000_000L, 3, '.'));
        assertEquals("8999999999999.999", Utils.toAmount(8_999_999_999_999_999L, 3, '.'));

        assertEquals("1", Utils.toAmount(1L, 0, '.'));
        assertEquals("1000", Utils.toAmount(1_000L, 0, '.'));
        assertEquals("8999999999000000", Utils.toAmount(8_999_999_999_000_000L, 0, '.'));
        assertEquals("8999999999999999", Utils.toAmount(8_999_999_999_999_999L, 0, '.'));
    }

    @Test
    void toAmount_Reverse() {
        assertEquals(1L, Utils.toAmount("0.000001", 6, '.'));
        assertEquals(1_000_000L, Utils.toAmount("1.000000", 6, '.'));
        assertEquals(8_999_999_999_000_000L, Utils.toAmount("8999999999.000000", 6, '.'));
        assertEquals(8_999_999_999_999_999L, Utils.toAmount("8999999999.999999", 6, '.'));

        assertEquals(1L, Utils.toAmount("0.001", 3, '.'));
        assertEquals(1_000L, Utils.toAmount("1.000", 3, '.'));
        assertEquals(8_999_999_999_000_000L, Utils.toAmount("8999999999000.000", 3, '.'));
        assertEquals(8_999_999_999_999_999L, Utils.toAmount("8999999999999.999", 3, '.'));

        assertEquals(1L, Utils.toAmount("1", 0, '.'));
        assertEquals(1_000L, Utils.toAmount("1000", 0, '.'));
        assertEquals(8_999_999_999_000_000L, Utils.toAmount("8999999999000000", 0, '.'));
        assertEquals(8_999_999_999_999_999L, Utils.toAmount("8999999999999999", 0, '.'));
    }
}