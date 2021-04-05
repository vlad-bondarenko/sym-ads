package sym.ads.core.model;

import java.util.Locale;

/**
 * Created by bondarenko.vlad@gmail.com on 30.10.18.
 */

public final class Category {

    private String name;
    private Locale locale;

    public Category(String name, Locale locale) {
        this.name = name;
        this.locale = locale;
    }

    public String getName() {
        return name;
    }

    public Locale getLocale() {
        return locale;
    }
}
