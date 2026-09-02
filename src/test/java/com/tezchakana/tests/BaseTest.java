package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.time.Duration;

public class BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);

    // Используется OrderErrorTest/OrderRetryTest для детерминированного отключения сети
    // перед оформлением заказа (см. toggleNetwork() ниже).
    protected static final String ADB_DEVICE = "emulator-5554";

    protected AndroidDriver driver;

    @BeforeMethod
    public void setUp() throws Exception {
        printPreflightChecklist();

        UiAutomator2Options options = new UiAutomator2Options()
                .setAppPackage(TestConfig.appPackage())
                .setAppActivity(TestConfig.appActivity())
                .setNoReset(true)
                .setAutoGrantPermissions(false)
                .setNewCommandTimeout(Duration.ofSeconds(120));

        driver = new AndroidDriver(new URI(TestConfig.appiumUrl()).toURL(), options);
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void printPreflightChecklist() throws IOException, InterruptedException {
        boolean emulatorUp = isEmulatorConnected();
        boolean appiumUp = isAppiumReachable();

        LOG.info("=== Preflight checklist ===");
        LOG.info("[{}] Android emulator visible in `adb devices`", emulatorUp ? "OK" : "FAIL");
        LOG.info("[{}] Appium server reachable at {}", appiumUp ? "OK" : "FAIL", TestConfig.appiumUrl());
        LOG.info("[..] App data is kept as-is (pm clear skipped to preserve login session)");
        LOG.info("===========================");

        if (!emulatorUp) {
            throw new IllegalStateException("No Android emulator/device found. Start the emulator and check `adb devices`.");
        }
        if (!appiumUp) {
            throw new IllegalStateException("Appium server is not reachable at " + TestConfig.appiumUrl() + ". Start the Appium server first.");
        }
    }

    private boolean isEmulatorConnected() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("adb", "devices").start();
        process.waitFor();
        String output = new String(process.getInputStream().readAllBytes());
        return output.lines().skip(1).anyMatch(line -> line.contains("\tdevice"));
    }

    private boolean isAppiumReachable() {
        try {
            URL statusUrl = new URI(TestConfig.appiumUrl() + "/status").toURL();
            HttpURLConnection connection = (HttpURLConnection) statusUrl.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode == 200;
        } catch (IOException | URISyntaxException e) {
            return false;
        }
    }

    // 2026-08-31: раньше `toggleNetwork(false)` (дублировался в OrderErrorTest и
    // OrderRetryTest) просто запускал `svc wifi/data disable` и СРАЗУ ЖЕ возвращался,
    // не дожидаясь, пока устройство реально применит отключение - живая проверка
    // показала, что между моментом, когда adb-команда возвращает управление, и
    // моментом, когда `/proc/net/route` на устройстве реально пустеет (сеть физически
    // недостижима - `ping` отдаёт "Network is unreachable", а не таймаут), проходит
    // до ~1 секунды. Тест, тапавший "Buyurtma qilish" сразу после вызова adb-команды
    // без этой задержки, дважды оставил РЕАЛЬНЫЕ подтверждённые заказы (TEZ00167/
    // TEZ00168, см. ORD-02/ORD-04 в docs/exploration-notes.md) - похоже, тап и запрос
    // на оформление успевали проскочить именно в это окно до фактического отключения.
    // Теперь метод явно ждёт подтверждения нужного состояния сети через опрос
    // `/proc/net/route`, а не полагается на то, что adb-команда вернулась.
    protected void toggleNetwork(boolean enabled) throws IOException, InterruptedException {
        String state = enabled ? "enable" : "disable";
        new ProcessBuilder("adb", "-s", ADB_DEVICE, "shell", "svc", "wifi", state).start().waitFor();
        new ProcessBuilder("adb", "-s", ADB_DEVICE, "shell", "svc", "data", state).start().waitFor();
        waitForNetworkState(enabled);
    }

    private void waitForNetworkState(boolean expectedUp) throws IOException, InterruptedException {
        long deadlineMillis = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadlineMillis) {
            if (isNetworkReachable() == expectedUp) {
                return;
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Сеть не подтвердила ожидаемое состояние ("
                + (expectedUp ? "включена" : "отключена") + ") за 10с через /proc/net/route - "
                + "дальнейшие шаги, зависящие от состояния сети (в т.ч. оформление заказа), "
                + "небезопасно продолжать");
    }

    // Пустая таблица маршрутизации (только заголовок, ни одной реальной строки) -
    // надёжный признак того, что сеть отключена физически, а не только "выглядит
    // отключённой" на уровне ConnectivityManager (та же проверка, что подтвердила
    // проблему вживую - см. toggleNetwork() выше).
    private boolean isNetworkReachable() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("adb", "-s", ADB_DEVICE, "shell", "cat", "/proc/net/route").start();
        process.waitFor();
        String output = new String(process.getInputStream().readAllBytes());
        return output.lines().count() > 1;
    }

}
