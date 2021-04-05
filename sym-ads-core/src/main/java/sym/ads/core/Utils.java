package sym.ads.core;

import com.google.gson.Gson;
import one.nio.util.URLEncoder;
import one.nio.util.Utf8;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by bondarenko.vlad@gmail.com on 26.10.18.
 */

public class Utils {

    public static final byte[] EMPTY = new byte[0];

    public static final Gson GSON = new Gson();

    public static String read(URL url) throws IOException {
        try (InputStream inputStream = url.openStream()) {
            return Utf8.toString(inputStream.readAllBytes());
        }
    }

    public static Map<String, List<String>> parseParameters(String body) {
        HashMap<String, List<String>> result = new HashMap<>();
        for (StringTokenizer tokenizer = new StringTokenizer(body, "&"); tokenizer.hasMoreTokens(); ) {
            String param = tokenizer.nextToken();
            int p = param.indexOf('=');
            if (p > 0) {
                result.computeIfAbsent(param.substring(0, p), s -> new ArrayList<>()).add(URLEncoder.decode(param.substring(p + 1)));
            }
        }

        return result;
    }

    public static String firstParam(Map<String, List<String>> params, String name) {
        List<String> list = params.get(name);
        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static int parseInt(String v) {
        if (v == null || v.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ignored) {
        }

        return 0;
    }

    public static long parseLong(String v) {
        if (v == null || v.isBlank()) {
            return 0;
        }

        try {
            return Long.parseLong(v);
        } catch (NumberFormatException ignored) {
        }

        return 0;
    }

    public static double parseDouble(String v) {
        if (v == null || v.isBlank()) {
            return 0;
        }

        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException ignored) {
        }

        return 0;
    }

    public static byte[] toBytesOrDefault(Object o) {
        return o == null ? EMPTY : Utf8.toBytes(String.valueOf(o));
    }

    public static byte[] toBytesOrDefault(String s) {
        return s == null ? EMPTY : Utf8.toBytes(s);
    }

    public static String toAmount(long v, int divisibility, char separator) {
        String s = String.valueOf(v);

        if (divisibility == 0) {
            return s;
        }

        int len = s.length();

        if (divisibility == len) {
            return s;
        }

        if (divisibility > len) {
            int diff = divisibility - len;
            char[] updatedArr = new char[len + diff + 2];
            s.getChars(0, len, updatedArr, diff + 2);
            updatedArr[0] = '0';
            updatedArr[1] = separator;
            for (int i = 2; i < diff + 2; i++) {
                updatedArr[i] = '0';
            }

            return new String(updatedArr);
        }

        char[] updatedArr = new char[len + 1];
        int pos = len - divisibility;
        s.getChars(0, pos, updatedArr, 0);
        updatedArr[pos] = separator;
        s.getChars(pos, len, updatedArr, pos + 1);

        return new String(updatedArr);
    }

    public static long toAmount(String s, int divisibility, char separator) {
        int pos = s.indexOf(separator);
        if (pos == -1) {
            return Long.parseLong(s) * (BigInteger.TEN.pow(divisibility).longValue());
        }

        int len = s.length();
        char[] updatedArr = new char[len - 1];
        s.getChars(0, pos, updatedArr, 0);
        s.getChars(pos + 1, len, updatedArr, pos);

        int div = len - pos - 1;
        if (div == divisibility) {
            return Long.parseLong(new String(updatedArr));
        }

        return Long.parseLong(new String(updatedArr)) * (BigInteger.TEN.pow(div).longValue());
    }

    public static byte[] toBytes(long v) {
        return ByteBuffer.allocate(8).putLong(v).array();
    }

    public static long toLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }

    public static long toLong(byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap(bytes, offset, length).getLong();
    }

    public static String getParameter(String query, String key) {
        int cur = 1;
        while (cur > 0) {
            int next = query.indexOf('&', cur);
            if (query.startsWith(key, cur)) {
                cur += key.length();
                return next > 0 ? query.substring(cur, next) : query.substring(cur);
            }
            cur = next + 1;
        }

        return null;
    }

    public static String getParameterEncoded(String query, String key) {
        String v = getParameter(query, key);

        return v == null ? null : URLEncoder.decode(v);
    }

}
