package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

public class SettingsScreen extends BaseScreen {

    // Все локаторы проверены live на реальном аккаунте 2026-08-27 в гостевом состоянии
    // (Sozlamalar доступна без логина).
    private static final By TIL_ROW =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Til\")");
    private static final By FAQ_ROW = AppiumBy.accessibilityId("Ko‘p beriladigan savollar");
    private static final By PRIVACY_ROW = AppiumBy.accessibilityId("Ilova maxfiylik siyosati");
    private static final By VERSION_TEXT =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionStartsWith(\"V \")");

    private static final By LANGUAGE_DIALOG_TITLE = AppiumBy.accessibilityId("Tilni tanlash");
    private static final By LANGUAGE_OPTION_UZBEK_LATIN = AppiumBy.accessibilityId("O‘zbekcha");
    private static final By LANGUAGE_OPTION_RUSSIAN = AppiumBy.accessibilityId("Русский");

    // Крестик закрытия шторки "Tilni tanlash" - без content-desc, тот же паттерн
    // coordinate-tap, что и BOTTOM_CTA в BaseScreen (см. bounds [933,1576][1080,1723]
    // на эталонном экране 1080x2400, центр x=1006/y=1649).
    private static final int LANGUAGE_DIALOG_CLOSE_REF_X = 1006;
    private static final int LANGUAGE_DIALOG_CLOSE_REF_Y = 1649;

    // FAQ и Privacy policy открываются ВНУТРИ приложения (в отличие от "Yordam",
    // который открывает внешний Chrome - см. PROF-A03 в docs/exploration-notes.md).
    private static final By FAQ_FIRST_QUESTION = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Buyurtmani qanday rasmiylashtiraman\")");
    private static final By PRIVACY_DIALOG_TITLE = AppiumBy.accessibilityId("Maxfiylik siyosati");
    private static final By PRIVACY_CLOSE_BUTTON = AppiumBy.accessibilityId("Yopish");

    public SettingsScreen(AndroidDriver driver) {
        super(driver);
    }

    // SET-01 (базовая часть) / SET-05: строка языка, FAQ, политика конфиденциальности
    // и версия отображаются на экране настроек.
    public SettingsScreen verifyScreenShown() {
        Assert.assertTrue(waitFor(TIL_ROW).isDisplayed(), "Строка \"Til\" не отображается в Sozlamalar");
        Assert.assertTrue(driver.findElement(FAQ_ROW).isDisplayed(), "\"Ko'p beriladigan savollar\" не отображается");
        Assert.assertTrue(driver.findElement(PRIVACY_ROW).isDisplayed(), "\"Ilova maxfiylik siyosati\" не отображается");
        Assert.assertTrue(driver.findElement(VERSION_TEXT).isDisplayed(), "Версия приложения не отображается");
        return this;
    }

    // SET-01: открытие выбора языка показывает шторку с текущим языком и минимум
    // "O'zbekcha"/"Русский" среди опций. НЕ подтверждаем выбор (не тапаем Tasdiqlash) -
    // при живой проверке 2026-08-27 смена языка на "Русский" оказалась нестабильной
    // (см. project-real-account-live-backend в памяти): интерфейс на Home сразу
    // отобразился по-русски, но заголовок "Til" в Sozlamalar сам вернулся к "O'zbekcha"
    // без какого-либо ручного отката с моей стороны. Автоматизировать реальное
    // подтверждение смены языка на живом аккаунте до выяснения причины этого отката -
    // рискованно (флаки). Отдельно: список языков в этой же живой проверке один раз
    // показал 4 варианта (+ English), второй раз - только 3 (без English) без каких-либо
    // действий с моей стороны между проверками, поэтому English сюда не включён как
    // обязательный.
    public SettingsScreen verifyLanguagePickerShowsOptions() {
        waitFor(TIL_ROW).click();
        Assert.assertTrue(waitFor(LANGUAGE_DIALOG_TITLE).isDisplayed(), "Шторка \"Tilni tanlash\" не открылась");
        Assert.assertTrue(driver.findElement(LANGUAGE_OPTION_UZBEK_LATIN).isDisplayed(), "Опция \"O'zbekcha\" не отображается");
        Assert.assertTrue(driver.findElement(LANGUAGE_OPTION_RUSSIAN).isDisplayed(), "Опция \"Русский\" не отображается");
        tapAt(scaledX(LANGUAGE_DIALOG_CLOSE_REF_X), scaledY(LANGUAGE_DIALOG_CLOSE_REF_Y));
        Assert.assertTrue(waitFor(TIL_ROW).isDisplayed(), "Экран Sozlamalar не восстановился после закрытия шторки языка");
        return this;
    }

    // SET-03: FAQ открывается ВНУТРИ приложения (аккордеон вопросов), а не во внешнем
    // браузере.
    public SettingsScreen verifyFaqOpensInApp() {
        waitFor(FAQ_ROW).click();
        Assert.assertTrue(waitFor(FAQ_FIRST_QUESTION).isDisplayed(), "Список FAQ не отобразился");
        driver.navigate().back();
        Assert.assertTrue(waitFor(TIL_ROW).isDisplayed(), "Экран Sozlamalar не восстановился после FAQ");
        return this;
    }

    // SET-04: политика конфиденциальности открывается ВНУТРИ приложения (модалка с
    // текстом и кнопкой "Yopish"), а не во внешнем браузере.
    public SettingsScreen verifyPrivacyPolicyOpensInApp() {
        waitFor(PRIVACY_ROW).click();
        Assert.assertTrue(waitFor(PRIVACY_DIALOG_TITLE).isDisplayed(), "Модалка политики конфиденциальности не открылась");
        waitFor(PRIVACY_CLOSE_BUTTON).click();
        Assert.assertTrue(waitFor(TIL_ROW).isDisplayed(), "Экран Sozlamalar не восстановился после политики конфиденциальности");
        return this;
    }
}
