package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

public class DetailsScreen extends BaseScreen {

    // "Mening tafsilotlarim" - все локаторы проверены live 2026-08-27 на реальном
    // авторизованном аккаунте. Номер телефона зашит намеренно - тот же реальный номер,
    // что и в TestConfig (909023162 -> +998 90 902 31 62), поле показывает его как
    // read-only content-desc (НЕ EditText), в отличие от Ism/E-mail.
    private static final By ISM_FIELD =
            AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\").instance(0)");
    private static final By PHONE_VALUE = AppiumBy.accessibilityId("+998 90 902 31 62");
    private static final By INSTAGRAM_BUTTON = AppiumBy.accessibilityId("Instagram qo'shish");
    private static final By TELEGRAM_BUTTON = AppiumBy.accessibilityId("Telegram qo'shish");

    public DetailsScreen(AndroidDriver driver) {
        super(driver);
    }

    // DET-01: Ism (редактируемый), Telefon (read-only), кнопки Instagram/Telegram
    // отображаются.
    public DetailsScreen verifyDetailsShown() {
        Assert.assertTrue(waitFor(ISM_FIELD).isDisplayed(), "Поле \"Ism\" не отображается");
        Assert.assertTrue(driver.findElement(PHONE_VALUE).isDisplayed(),
                "Номер телефона не отображается как read-only значение");
        Assert.assertTrue(driver.findElement(INSTAGRAM_BUTTON).isDisplayed(), "Кнопка \"Instagram qo'shish\" не отображается");
        Assert.assertTrue(driver.findElement(TELEGRAM_BUTTON).isDisplayed(), "Кнопка \"Telegram qo'shish\" не отображается");
        return this;
    }

    // DET-02 (не автоматизирован): "Saqlash" визуально активируется после
    // редактирования (проверено вручную 2026-08-27 - серая кнопка становится красной
    // после ввода в E-mail), но Flutter-виджет не выставляет `enabled`/`clickable` в
    // accessibility-дереве (оба атрибута остаются true в обоих состояниях) - надёжного
    // непиксельного сигнала для автоматической проверки нет. Тапать реальный "Saqlash"
    // на живом аккаунте ради проверки состояния - лишний риск. См.
    // project-real-account-live-backend в памяти проекта.
}
