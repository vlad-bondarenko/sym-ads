package sym.ads.core;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Properties;

public class Settings extends BaseClass {

    private static final Settings INSTANCE = new Settings();

    private final Properties properties = new Properties();

    public static Settings getInstance() {
        return INSTANCE;
    }

    private Settings() {
        try {
//            String propertiesPath = System.getProperty("app.properties", "/app-local.properties");
            String propertiesPath = System.getProperty("app.properties", "/app-test.properties");
//            String propertiesPath = System.getProperty("app.properties", "/app-test-private.properties");

            log.info("Use {}", propertiesPath);

            properties.load(getClass().getResourceAsStream(propertiesPath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Role role() {
        String role = properties.getProperty("role");

        try {
            return Role.valueOf(role);
        } catch (Exception e) {
            log.error(e.toString(), e);

            throw new RuntimeException("Wrong '" + role + "', available roles: " + Arrays.toString(Role.values()));
        }
    }

    public String nodeUrl() {
        return properties.getProperty("node.url");
    }

/*
    public String address() {
        return properties.getProperty("address");
    }
*/

    public String privateKey() {
        return properties.getProperty("key.private");
    }

    public BigInteger maxFee() {
        return BigInteger.valueOf(Long.parseLong(properties.getProperty("transaction.maxFee")));
    }

}
