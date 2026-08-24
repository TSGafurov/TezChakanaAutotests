package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginScreen extends BaseScreen {

    // Элементы "Kirish" на экране входа в профиль имеют clickable="false" в дереве
    // accessibility - рабочего локатора нет, поэтому тапаем по координатам центра нижней
    // кнопки "Kirish" (bounds [485,2193][595,2240] на экране 1080x2400 из снимка).
    private static final int LOGIN_PROMPT_KIRISH_REF_X = 540;
    private static final int LOGIN_PROMPT_KIRISH_REF_Y = 2216;

    private static final By PHONE_INPUT_FIELD = AppiumBy.className("android.widget.EditText");
    private static final By CONTINUE_BUTTON = AppiumBy.accessibilityId("Telefon raqamini kiriting\nDavom etish");

    public LoginScreen(AndroidDriver driver) {
        super(driver);
    }

    // Дожидаемся появления диалога (по некликабельному, но детектируемому узлу "Kirish"),
    // затем ждём завершения анимации bottom sheet перед тапом по координатам - иначе тап
    // попадает мимо настоящей позиции кнопки.
    public LoginScreen confirmPrompt() {
        By kirishNode = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Kirish\")");
        new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> !d.findElements(kirishNode).isEmpty());
        sleep(Duration.ofMillis(500));
        tapAt(scaledX(LOGIN_PROMPT_KIRISH_REF_X), scaledY(LOGIN_PROMPT_KIRISH_REF_Y));
        return this;
    }

    public OtpScreen submitPhoneNumber(String phoneDigits) {
        waitFor(PHONE_INPUT_FIELD).sendKeys(phoneDigits);
        waitFor(CONTINUE_BUTTON);
        tapBottomCta();
        return new OtpScreen(driver);
    }
}
