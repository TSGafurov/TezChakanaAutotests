package com.tezchakana.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public final class TestConfig {

    private static final Properties PROPERTIES = load();
    private static String resolvedUdid;

    private TestConfig() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties не найден в classpath - "
                        + "скопируй src/test/resources/config.properties.example в "
                        + "src/test/resources/config.properties и подставь реальные phone.number/otp.code "
                        + "(файл не хранится в git - см. .gitignore)");
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

    // Статический тестовый OTP убран бэкендом (2026-09-16) - otp.code=manual (или пустое
    // значение) означает, что код из SMS вводит человек прямо на устройстве, а тест
    // только ждёт, пока экран OTP закроется (см. OtpScreen.enterCode()).
    public static boolean isManualOtp() {
        String code = otpCode();
        return code == null || code.isBlank() || code.equalsIgnoreCase("manual");
    }

    public static int manualOtpTimeoutSeconds() {
        String value = get("otp.manual.timeout.seconds");
        return value == null || value.isBlank() ? 180 : Integer.parseInt(value.trim());
    }

    // -Ddevice.udid=... / device.udid в config.properties; если не задан - берётся
    // единственное подключённое устройство из `adb devices` (эмулятор или реальный
    // телефон). При нескольких устройствах без явного udid - падаем явно, а не угадываем
    // (см. комментарий про ambiguous-device в BaseTest.setUp()).
    public static synchronized String deviceUdid() {
        if (resolvedUdid != null) {
            return resolvedUdid;
        }
        String configured = get("device.udid");
        if (configured != null && !configured.isBlank()) {
            resolvedUdid = configured.trim();
            return resolvedUdid;
        }
        try {
            Process process = new ProcessBuilder("adb", "devices").start();
            process.waitFor();
            List<String> devices = new String(process.getInputStream().readAllBytes()).lines()
                    .skip(1)
                    .filter(line -> line.endsWith("\tdevice"))
                    .map(line -> line.substring(0, line.indexOf('\t')))
                    .toList();
            if (devices.size() != 1) {
                throw new IllegalStateException("Ожидалось ровно одно устройство в `adb devices`, найдено "
                        + devices + " - укажи нужное явно через -Ddevice.udid=...");
            }
            resolvedUdid = devices.get(0);
            return resolvedUdid;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось выполнить `adb devices`", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Прервано ожидание `adb devices`", e);
        }
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
