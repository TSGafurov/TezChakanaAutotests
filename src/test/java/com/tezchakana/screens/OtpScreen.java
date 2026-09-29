package com.tezchakana.screens;

import com.tezchakana.config.TestConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class OtpScreen extends BaseScreen {

    private static final By OTP_INPUT_FIELD = AppiumBy.className("android.widget.EditText");
    private static final By CONFIRM_OTP_BUTTON = AppiumBy.accessibilityId("Tasdiqlash");
    private static final By OTP_HEADER = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"kodni kiriting\")");

    public OtpScreen(AndroidDriver driver) {
        super(driver);
    }

    // Поле OTP - это 6-ячеечный pin-код виджет: Appium sendKeys() (ACTION_SET_TEXT) не
    // отображается в ячейках вообще, в отличие от `adb shell input text`, который
    // эмулирует реальные IME-события. Но даже так - между появлением EditText в дереве
    // (после отправки SMS, с сетевой задержкой) и реальной готовностью input connection
    // проходит время, поэтому ждём характерный заголовок экрана и делаем паузу перед
    // вводом (при ручной проверке ввод сразу после навигации не срабатывал). Поле уже
    // сфокусировано при открытии экрана - тап перед вводом не нужен и сбивает фокус.
    //
    // Ручной режим (TestConfig.isManualOtp(), otp.code=manual): статического тестового
    // кода больше нет, реальный SMS-код вводит человек на устройстве и сам жмёт
    // "Tasdiqlash" - тест лишь ждёт, пока экран OTP закроется.
    public void enterCode(String code) {
        new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> !d.findElements(OTP_HEADER).isEmpty());
        if (TestConfig.isManualOtp()) {
            waitForManualEntry();
            return;
        }
        waitFor(OTP_INPUT_FIELD);
        sleep(Duration.ofSeconds(1));
        typeViaAdb(code);
    }

    public PermissionsScreen confirmCode() {
        if (TestConfig.isManualOtp()) {
            return new PermissionsScreen(driver);
        }
        waitFor(CONFIRM_OTP_BUTTON);
        tapBottomCta();
        return new PermissionsScreen(driver);
    }

    private void waitForManualEntry() {
        int timeout = TestConfig.manualOtpTimeoutSeconds();
        String banner = "\n==================================================\n"
                + ">>> ВВЕДИТЕ OTP ИЗ SMS НА УСТРОЙСТВЕ И НАЖМИТЕ \"Tasdiqlash\" (" + timeout + " c) <<<\n"
                + "==================================================";
        System.out.println(banner);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeout), Duration.ofSeconds(1))
                    .until(d -> d.findElements(OTP_HEADER).isEmpty());
        } catch (TimeoutException e) {
            throw new IllegalStateException("OTP не был введён вручную за " + timeout + " c", e);
        }
    }
}
