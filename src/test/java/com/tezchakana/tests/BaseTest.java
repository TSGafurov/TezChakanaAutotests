package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.time.Duration;

public class BaseTest {

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

        System.out.println("=== Preflight checklist ===");
        System.out.println("[" + (emulatorUp ? "OK" : "FAIL") + "] Android emulator visible in `adb devices`");
        System.out.println("[" + (appiumUp ? "OK" : "FAIL") + "] Appium server reachable at " + TestConfig.appiumUrl());
        System.out.println("[..] App data is kept as-is (pm clear skipped to preserve login session)");
        System.out.println("===========================");

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

}
