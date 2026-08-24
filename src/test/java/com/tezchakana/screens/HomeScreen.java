package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

public class HomeScreen extends BaseScreen {

    private static final By BAZAR_TAB = AppiumBy.accessibilityId("Bazar");

    // Нижняя вкладка "Bosh sahifa" (иконка домика) - без content-desc/resource-id (не
    // находится ни через accessibility id, ни через uiautomator dump на этом экране),
    // поэтому тап по координатам. Позиция снята со снимка 1080x2400.
    private static final int HOME_TAB_REF_X = 150;
    private static final int HOME_TAB_REF_Y = 2313;

    public HomeScreen(AndroidDriver driver) {
        super(driver);
    }

    public BazarScreen openBazarTab() {
        returnToHomeScreen();
        waitFor(BAZAR_TAB).click();
        return new BazarScreen(driver);
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
