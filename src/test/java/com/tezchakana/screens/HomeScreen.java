package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

import java.time.Duration;

public class HomeScreen extends BaseScreen {

    private static final By BAZAR_TAB = AppiumBy.accessibilityId("Bazar");
    private static final By HAMMASI_CHIP = AppiumBy.accessibilityId("Hammasi");
    private static final By GULLAR_CHIP = AppiumBy.accessibilityId("Gullar");
    private static final By KAFE_CHIP = AppiumBy.accessibilityId("Kafe");
    private static final By QANDOLAT_CHIP = AppiumBy.accessibilityId("Qandolat");

    // Заголовок списка магазинов содержит специфичный апостроф (do‘konlar), матчим по
    // "atrofdagi", чтобы не зависеть от того, какой именно символ апострофа use'ится.
    private static final By STORE_LIST_HEADER =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"atrofdagi\")");

    // Нижняя вкладка "Bosh sahifa" (иконка домика) - без content-desc/resource-id (не
    // находится ни через accessibility id, ни через uiautomator dump на этом экране),
    // поэтому тап по координатам. Позиция снята со снимка 1080x2400.
    // Y скорректирован 2026-08-25: исходное значение 2313 било ниже реальных bounds
    // иконки (проверено uiautomator dump на эмуляторе 1080x2400: [42,2224][221,2298],
    // центр y=2261) - из-за этого returnToHomeScreen() иногда не долетал до Home за
    // отведённые 5 тапов.
    private static final int HOME_TAB_REF_X = 150;
    private static final int HOME_TAB_REF_Y = 2261;

    public HomeScreen(AndroidDriver driver) {
        super(driver);
    }

    public BazarScreen openBazarTab() {
        returnToHomeScreen();
        waitFor(BAZAR_TAB).click();
        return new BazarScreen(driver);
    }

    // HOME-01: баннер не проверяем - это картинка без content-desc в дереве
    // доступности (карусель Flutter-виджетов без semantics-меток), стабильного
    // локатора для него нет.
    public HomeScreen verifyHomeLoaded() {
        returnToHomeScreen();
        Assert.assertTrue(waitFor(HAMMASI_CHIP).isDisplayed(), "Чип \"Hammasi\" не отображается на Home");
        Assert.assertTrue(driver.findElement(BAZAR_TAB).isDisplayed(), "Чип \"Bazar\" не отображается на Home");
        Assert.assertTrue(driver.findElement(GULLAR_CHIP).isDisplayed(), "Чип \"Gullar\" не отображается на Home");
        Assert.assertTrue(driver.findElement(KAFE_CHIP).isDisplayed(), "Чип \"Kafe\" не отображается на Home");
        Assert.assertTrue(driver.findElement(QANDOLAT_CHIP).isDisplayed(), "Чип \"Qandolat\" не отображается на Home");
        Assert.assertTrue(waitFor(STORE_LIST_HEADER).isDisplayed(), "Список \"Yaqin atrofdagi do'konlar\" не отображается на Home");
        return this;
    }

    // noReset(true) сохраняет данные приложения, но НЕ сбрасывает in-app навигацию -
    // новая Appium-сессия просто подхватывается к тому экрану, на котором предыдущая
    // сессия/разведка оставила приложение (может быть где угодно, не обязательно Home).
    // Тап по нижней вкладке "Home" не прыгает сразу на глобальный Home, а сворачивает
    // текущий стек экранов по одному уровню за тап (подтверждено вручную: из категории
    // товаров первый тап приводит на корень магазина, только следующий - на Home),
    // поэтому тапаем повторно, пока не появится вкладка "Bazar".
    private void returnToHomeScreen() {
        int maxTaps = 5;
        while (driver.findElements(BAZAR_TAB).isEmpty() && maxTaps-- > 0) {
            tapAt(scaledX(HOME_TAB_REF_X), scaledY(HOME_TAB_REF_Y));
            sleep(Duration.ofMillis(700));
        }
    }
}
