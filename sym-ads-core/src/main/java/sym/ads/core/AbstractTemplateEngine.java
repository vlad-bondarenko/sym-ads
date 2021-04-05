package sym.ads.core;

import one.nio.util.Utf8;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static one.nio.util.Utf8.toBytes;

public abstract class AbstractTemplateEngine extends BaseClass {

    private static final Pattern PATTERN = Pattern.compile("((?s).*?)[$][{](\\w+[.]*\\w*)[}]");

    private final Template[] templates;

    protected AbstractTemplateEngine() {
        Path[] paths = templatePaths();
        templates = new Template[paths.length];
        try {
            for (int i = 0, pathsLength = paths.length; i < pathsLength; i++) {
                templates[i] = applyTemplate(Utf8.toString(Files.readAllBytes(paths[i])));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Path[] templatePaths();

    private static Template applyTemplate(String content) {
        Matcher matcher = PATTERN.matcher(content);

        int e = 0;
        ArrayList<byte[]> f = new ArrayList<>();
        ArrayList<String> n = new ArrayList<>();
        while (matcher.find()) {
            f.add(toBytes(matcher.group(1)));
            n.add(matcher.group(2));
            e = matcher.end();
        }
        f.add(toBytes(content.substring(e)));

        return new Template(f.toArray(new byte[f.size()][]), n.toArray(new String[0]));
    }

    public <T extends OutputStream> T render(int idx, T outputStream, HashMap<String, byte[]> model) throws IOException {
        return render(idx, outputStream, model, null);
    }

    public <T extends OutputStream> T render(int idx, T outputStream, HashMap<String, byte[]> model, BiPredicate<String, T> listBiPredicate) throws IOException {
        byte[][] fragments = templates[idx].fragments;
        String[] names = templates[idx].names;
        byte[] bytes = null;
        if (fragments.length > 1) {
            for (int i = 0, fragmentsLength = fragments.length - 1; i < fragmentsLength; i++) {
                outputStream.write(fragments[i], 0, fragments[i].length);

                if (i < names.length) {
                    if (model != null) {
                        bytes = model.get(names[i]);
                    }

                    if (listBiPredicate != null && listBiPredicate.test(names[i], outputStream)) {
                        continue;
                    }

                    if (bytes == null) {
                        bytes = toBytes("${" + names[i] + "}");
                    }

                    outputStream.write(bytes, 0, bytes.length);
                }
            }

            outputStream.write(fragments[fragments.length - 1], 0, fragments[fragments.length - 1].length);
        } else {
/*
            if (names.length > 1) {
                throw new UnsupportedOperationException();
            }
*/

            outputStream.write(fragments[0], 0, fragments[0].length);
            if (names.length > 0) {
                bytes = model.get(names[0]);

                if (bytes == null) {
                    bytes = toBytes("${" + names[0] + "}");
                }

                outputStream.write(bytes, 0, bytes.length);
            }
        }

        return outputStream;
    }

    private static class Template {
        byte[][] fragments;
        String[] names;

        Template(byte[][] fragments, String[] names) {
            this.fragments = fragments;
            this.names = names;
        }
    }
}
