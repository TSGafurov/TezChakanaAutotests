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
    public StoreScreen openStore(String storeNameContains) {
        By storeCard = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + storeNameContains + "\").clickable(true)");
        waitFor(storeCard).click();
        return new StoreScreen(driver);
    }
}
