package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class OtpScreen extends BaseScreen {

    private static final By OTP_INPUT_FIELD = AppiumBy.className("android.widget.EditText");
    private static final By CONFIRM_OTP_BUTTON = AppiumBy.accessibilityId("Tasdiqlash");

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
    public void enterCode(String code) {
        By otpHeader = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"kodni kiriting\")");
        new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> !d.findElements(otpHeader).isEmpty());
        waitFor(OTP_INPUT_FIELD);
        sleep(Duration.ofSeconds(1));
        typeViaAdb(code);
    }

    public PermissionsScreen confirmCode() {
        waitFor(CONFIRM_OTP_BUTTON);
        tapBottomCta();
        return new PermissionsScreen(driver);
    }
}
