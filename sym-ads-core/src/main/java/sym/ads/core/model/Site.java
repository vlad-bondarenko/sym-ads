package sym.ads.core.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by bondarenko.vlad@gmail.com on 17.12.2019.
 */

public final class Site implements Serializable {

    private final String url;
    private final String desc;
    private final String name;
    private final int category;
    private final int linkSize;
    private final String separator;
    private final int length;

    public Site(String url, String desc, String name, int category, int linkSize, String separator, int length) {
        this.url = url;
        this.desc = desc;
        this.name = name;
        this.category = category;
        this.linkSize = linkSize;
        this.separator = separator;
        this.length = length;
    }

    public String getUrl() {
        return url;
    }

    public String getDesc() {
        return desc;
    }

    public String getName() {
        return name;
    }

    public int getCategory() {
        return category;
    }

    public int getLinkSize() {
        return linkSize;
    }

    public String getSeparator() {
        return separator;
    }

    public int getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return category == site.category &&
                linkSize == site.linkSize &&
                length == site.length &&
                url.equals(site.url) &&
                Objects.equals(desc, site.desc) &&
                Objects.equals(name, site.name) &&
                Objects.equals(separator, site.separator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, desc, name, category, linkSize, separator, length);
    }

    @Override
    public String toString() {
        return "Site{" +
                "url='" + url + '\'' +
                ", desc='" + desc + '\'' +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", linkSize=" + linkSize +
                ", separator='" + separator + '\'' +
                ", length=" + length +
                '}';
    }
}
