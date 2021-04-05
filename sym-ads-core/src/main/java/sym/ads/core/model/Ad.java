package sym.ads.core.model;

import java.io.Serializable;
import java.util.Objects;

public final class Ad implements Serializable {

    private final String url;
    private final String desc;
    private final String name;
    private final int category;
    private final long price;
    private String country;
    private int fromHour;
    private int toHour;
    private int fromWeek;
    private int toWeek;
    private long bl;

    public Ad(String url, String desc, String name, int category, long price) {
        this.url = url;
        this.desc = desc;
        this.name = name;
        this.category = category;
        this.price = price;
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

    public long getPrice() {
        return price;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getFromHour() {
        return fromHour;
    }

    public void setFromHour(int fromHour) {
        this.fromHour = fromHour;
    }

    public int getToHour() {
        return toHour;
    }

    public void setToHour(int toHour) {
        this.toHour = toHour;
    }

    public int getFromWeek() {
        return fromWeek;
    }

    public void setFromWeek(int fromWeek) {
        this.fromWeek = fromWeek;
    }

    public int getToWeek() {
        return toWeek;
    }

    public void setToWeek(int toWeek) {
        this.toWeek = toWeek;
    }

    public long getBl() {
        return bl;
    }

    public void setBl(long bl) {
        this.bl = bl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ad ad = (Ad) o;
        return category == ad.category &&
                price == ad.price &&
                fromHour == ad.fromHour &&
                toHour == ad.toHour &&
                fromWeek == ad.fromWeek &&
                toWeek == ad.toWeek &&
                bl == ad.bl &&
                url.equals(ad.url) &&
                Objects.equals(desc, ad.desc) &&
                Objects.equals(name, ad.name) &&
                Objects.equals(country, ad.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, desc, name, category, price, country, fromHour, toHour, fromWeek, toWeek, bl);
    }

    @Override
    public String toString() {
        return "Ad{" +
                "url='" + url + '\'' +
                ", desc='" + desc + '\'' +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", price=" + price +
                ", country='" + country + '\'' +
                ", fromHour=" + fromHour +
                ", toHour=" + toHour +
                ", fromWeek=" + fromWeek +
                ", toWeek=" + toWeek +
                ", bl=" + bl +
                '}';
    }
}
