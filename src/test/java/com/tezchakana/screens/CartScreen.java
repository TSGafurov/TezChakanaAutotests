package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

public class CartScreen extends BaseScreen {

    // Мини-корзина (bottom sheet), открывается после тапа по нижнему бару суммы.
    private static final By PROCEED_TO_CHECKOUT = AppiumBy.accessibilityId("Savat\nTo'lovga o'tish");

    public CartScreen(AndroidDriver driver) {
        super(driver);
    }

    public LoginScreen proceedToCheckout() {
        waitFor(PROCEED_TO_CHECKOUT);
        tapBottomCta();
        return new LoginScreen(driver);
    }
}
