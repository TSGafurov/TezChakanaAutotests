package com.tezchakana.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Test data and environment settings, loaded from config.properties on the
 * classpath. Every key can be overridden with a matching -D system property
 * (e.g. -Dphone.number=... to point the suite at a different account) without
 * touching code.
 */
public final class TestConfig {

    private static final Properties PROPERTIES = load();

    private TestConfig() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties не найден в classpath");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить config.properties", e);
        }
        return properties;
    }

    private static String get(String key) {
        return System.getProperty(key, PROPERTIES.getProperty(key));
    }

    public static String appiumUrl() {
        return get("appium.url");
    }

    public static String appPackage() {
        return get("app.package");
    }

    public static String appActivity() {
        return get("app.activity");
    }

    public static String phoneNumber() {
        return get("phone.number");
    }

    public static String otpCode() {
        return get("otp.code");
    }

    public static String storeName() {
        return get("store.name");
    }

    public static String waterCategoryLabel() {
        return get("category.water");
    }

    public static String productName() {
        return get("product.name");
    }

    public static String groceryCategoryLabel() {
        return get("category.grocery");
    }

    public static String groceryProductName() {
        return get("product.grocery");
    }

    public static String defaultAddressLabel() {
        return get("address.default");
    }

    public static String alternateAddressLabel() {
        return get("address.alternate");
    }

    public static String chustSearchQuery() {
        return get("address.search.chust");
    }

    public static String noCoverageSearchQuery() {
        return get("address.search.no.coverage");
    }

    public static int referenceScreenWidth() {
        return Integer.parseInt(get("reference.screen.width"));
    }

    public static int referenceScreenHeight() {
        return Integer.parseInt(get("reference.screen.height"));
    }
}
