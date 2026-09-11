package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

public class BazarScreen extends BaseScreen {

    public BazarScreen(AndroidDriver driver) {
        super(driver);
    }

    // Метка магазина включает часы работы ("Ochiq\nEco Bazar\n08:00 - 19:00"), поэтому
    // матчим по частичному описанию, а не по точной строке.
    //
    // 2026-09-11: обнаружено вживую на полном прогоне testng.xml - `waitFor(storeCard)`
    // без скролла падал по таймауту, если список магазинов под "Bazar" оказывался
    // прокручен дальше нужной карточки (NoReset сохраняет позицию скролла между
    // сессиями, см. HomeScreen.scrollHomeContentToTop()) - искомая карточка просто не
    // рендерилась в дереве доступности. UiScrollable.scrollIntoView() сам докручивает до
    // нужного элемента независимо от текущей позиции, вместо того чтобы полагаться на
    // то, что он уже виден.
    public StoreScreen openStore(String storeNameContains) {
        By storeCard = AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                        + "new UiSelector().descriptionContains(\"" + storeNameContains + "\").clickable(true))");
        waitFor(storeCard).click();
        return new StoreScreen(driver);
    }
}
