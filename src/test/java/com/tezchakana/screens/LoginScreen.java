package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginScreen extends BaseScreen {

    // Элементы "Kirish" на экране входа в профиль имеют clickable="false" в дереве
    // accessibility - рабочего локатора нет, поэтому тапаем по координатам центра нижней
    // кнопки "Kirish" (bounds [485,2193][595,2240] на экране 1080x2400 из снимка).
    private static final int LOGIN_PROMPT_KIRISH_REF_X = 540;
    private static final int LOGIN_PROMPT_KIRISH_REF_Y = 2216;

    private static final By KIRISH_NODE = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Kirish\")");
    private static final By PHONE_INPUT_FIELD = AppiumBy.className("android.widget.EditText");
    private static final By CONTINUE_BUTTON = AppiumBy.accessibilityId("Telefon raqamini kiriting\nDavom etish");

    // "Davom etish" на этом экране - НЕ стандартный нижний merged-CTA (BaseScreen.tapBottomCta(),
    // y=2286): кнопка приклеена сразу под полем ввода телефона, а не к нижнему краю экрана,
    // ниже неё пустое место. Подтверждено вживую 2026-08-29 (первый реальный прогон гостевого
    // логина после снятия ограничения на OTP-автоматизацию, см. [[tezchakana-login-preference]]):
    // tapBottomCta() промахивался мимо кнопки, "Davom etish" не срабатывал. Позиция не зависит
    // от видимости клавиатуры (сверена на снимках с открытой и закрытой клавиатурой - идентична).
    private static final int PHONE_CONTINUE_REF_X = 540;
    private static final int PHONE_CONTINUE_REF_Y = 1419;

    public LoginScreen(AndroidDriver driver) {
        super(driver);
    }

    // CartScreen.proceedToCheckout() возвращает LoginScreen безусловно, но для уже
    // авторизованного аккаунта тап по "To'lovga o'tish" ведёт сразу на экран чекаута
    // (PaymentScreen), минуя этот промпт вовсе - см. CheckoutFlowTest, который должен
    // отличать эти два случая (CHK-01, гостевой логин, тестируется именно этим классом).
    // Короткий таймаут (не WAIT_TIMEOUT) - на авторизованном старте узла "Kirish" не
    // появится совсем, и не стоит ждать 15 секунд впустую при каждом прогоне.
    public boolean isLoginPromptShown() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(d -> !d.findElements(KIRISH_NODE).isEmpty());
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Дожидаемся появления диалога (по некликабельному, но детектируемому узлу "Kirish"),
    // затем ждём завершения анимации bottom sheet перед тапом по координатам - иначе тап
    // попадает мимо настоящей позиции кнопки.
    public LoginScreen confirmPrompt() {
        new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> !d.findElements(KIRISH_NODE).isEmpty());
        sleep(Duration.ofMillis(500));
        tapAt(scaledX(LOGIN_PROMPT_KIRISH_REF_X), scaledY(LOGIN_PROMPT_KIRISH_REF_Y));
        return this;
    }

    public OtpScreen submitPhoneNumber(String phoneDigits) {
        waitFor(PHONE_INPUT_FIELD).sendKeys(phoneDigits);
        waitFor(CONTINUE_BUTTON);
        tapAt(scaledX(PHONE_CONTINUE_REF_X), scaledY(PHONE_CONTINUE_REF_Y));
        return new OtpScreen(driver);
    }
}
