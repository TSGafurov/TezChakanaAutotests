package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

/**
 * "Buyurtmalar" (список заказов): ORDH-01. Проверено вживую 2026-08-28 - открывается
 * из Profile (см. ProfileScreen.openOrders()), показывает вкладки "Faol"/"Tugallangan"
 * и карточки заказов ("Buyurtma raqami: ...\nTasdiqlandi\n..."), тап по карточке ведёт
 * на OrderDetailsScreen.
 */
public class OrdersScreen extends BaseScreen {

    // Заголовок "Buyurtmalar" сознательно не используем как маркер загрузки экрана -
    // тот же текст уже встречается как пункт меню на Profile (см. ProfileScreen про
    // тот же класс риска неоднозначности, что и с "Sozlamalar" в SettingsScreen).
    private static final By ACTIVE_TAB =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Faol\")");
    private static final By COMPLETED_TAB =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Tugallangan\")");

    // Карточка заказа - "Buyurtma raqami:\n<номер>\n<статус>\nBuyurtma sanasi:\n<дата>\n<сумма>
    // uzs". Матчим по префиксу вместо конкретного номера/суммы - это реальные данные
    // реального аккаунта, тест не должен на них завязываться.
    private static final By ORDER_CARD =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Buyurtma raqami:\")");

    public OrdersScreen(AndroidDriver driver) {
        super(driver);
    }

    // ORDH-01: список активных заказов показывает вкладки и хотя бы одну карточку.
    public OrdersScreen verifyOrdersShown() {
        Assert.assertTrue(waitFor(ACTIVE_TAB).isDisplayed(), "Вкладка \"Faol\" не отображается");
        Assert.assertTrue(driver.findElement(COMPLETED_TAB).isDisplayed(), "Вкладка \"Tugallangan\" не отображается");
        Assert.assertFalse(driver.findElements(ORDER_CARD).isEmpty(), "Ни одной карточки заказа не отображается");
        return this;
    }

    public OrderDetailsScreen openFirstOrder() {
        waitFor(ORDER_CARD).click();
        return new OrderDetailsScreen(driver);
    }

    // Самый свежий заказ - первая карточка во вкладке "Faol" (новые заказы реального
    // аккаунта появляются сверху списка, подтверждено вживую). Используется как
    // сигнатура "ничего нового не появилось" в OrderErrorTest/OrderRetryTest -
    // см. комментарий про ORD-02/ORD-04 в docs/exploration-notes.md о том, почему
    // одного лишь ассерта на error-state на экране оплаты недостаточно, чтобы доверять,
    // что реальный заказ не был создан.
    public String topOrderNumber() {
        String cardText = waitFor(ORDER_CARD).getAttribute("content-desc");
        String[] lines = cardText.split("\n");
        return lines.length > 1 ? lines[1].trim() : cardText;
    }
}
