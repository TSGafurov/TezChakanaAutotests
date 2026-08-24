package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

public class PaymentScreen extends BaseScreen {

    private static final By PAYMENT_METHOD_ROW = AppiumBy.accessibilityId("To'lov usuli\nTo'lov usulini tanlang");
    private static final By CASH_PAYMENT_OPTION = AppiumBy.accessibilityId("Naqd pul\nNaqd pul");
    private static final By PLACE_ORDER_BUTTON = AppiumBy.accessibilityId("Xarid qilish\nBuyurtma qilish");
    private static final By ORDER_SUCCESS_INDICATOR =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Buyurtma qabul qilindi\")");

    public PaymentScreen(AndroidDriver driver) {
        super(driver);
    }

    public PaymentScreen selectCashPayment() {
        waitFor(PAYMENT_METHOD_ROW).click();
        waitFor(CASH_PAYMENT_OPTION).click();
        return this;
    }

    // Тоже смерженная на весь экран кнопка - см. комментарий у tapBottomCta() в BaseScreen.
    public PaymentScreen placeOrder() {
        waitFor(PLACE_ORDER_BUTTON);
        tapBottomCta();
        return this;
    }

    public void verifyOrderSuccess() {
        WebElement successElement = waitFor(ORDER_SUCCESS_INDICATOR);
        Assert.assertTrue(successElement.isDisplayed(), "Экран подтверждения заказа не отобразился");
    }
}
