package com.tezchakana.screens;

import com.tezchakana.config.TestConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

public class PaymentScreen extends BaseScreen {

    // descriptionContains, а не точный accessibility id: после выбора способа оплаты
    // content-desc строки меняется с "...To'lov usulini tanlang" на "...Naqd pul\nNaqd
    // pul" (см. CASH_PAYMENT_SELECTED_INDICATOR) - точный id ловил бы только
    // неавыбранное состояние и падал бы, если приложение уже помнит выбор с прошлого
    // запуска теста (NoReset(true) сохраняет это между запусками).
    private static final By PAYMENT_METHOD_ROW =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"To'lov usuli\")");
    private static final By CASH_PAYMENT_OPTION = AppiumBy.accessibilityId("Naqd pul\nNaqd pul");
    private static final By PLACE_ORDER_BUTTON = AppiumBy.accessibilityId("Xarid qilish\nBuyurtma qilish");
    private static final By ORDER_SUCCESS_INDICATOR =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Buyurtma qabul qilindi\")");

    // CHK-03: заголовок и строки блока "Yetkazib berish tafsilotlari" - каждая строка
    // смерженный узел (заголовок+значение), как и везде в приложении.
    private static final By DELIVERY_DETAILS_HEADER = AppiumBy.accessibilityId("Yetkazib berish tafsilotlari");
    private static final By ADDRESS_ROW =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Manzilga kuryer orqali\")");
    private static final By ETA_ROW = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Yaqin orada\")");
    private static final By COURIER_COMMENT_ROW = AppiumBy.accessibilityId("Kuryerga izoh qoldirish");

    // CHK-05: после выбора способа оплаты PAYMENT_METHOD_ROW (то же название, что и до
    // выбора) меняет content-desc с "To'lov usuli\nTo'lov usulini tanlang" на
    // "To'lov usuli\nNaqd pul\nNaqd pul" - точный accessibility id после выбора не
    // задаём отдельной константой (он динамический для других способов оплаты),
    // проверяем только по подстроке "Naqd pul", уникальной на этом экране после выбора.
    private static final By CASH_PAYMENT_SELECTED_INDICATOR =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Naqd pul\")");

    public PaymentScreen(AndroidDriver driver) {
        super(driver);
    }

    public PaymentScreen selectCashPayment() {
        waitFor(PAYMENT_METHOD_ROW).click();
        waitFor(CASH_PAYMENT_OPTION).click();
        // Выбор варианта в шторке - смерженный CTA "To'lov usuli\nTasdiqlash" на весь
        // экран, как и другие такие узлы в приложении (см. BaseScreen.tapBottomCta) -
        // без этого тапа шторка не закрывается и выбор не подтверждается.
        tapBottomCta();
        return this;
    }

    // CHK-03: блок доставки (адрес/ETA/получатель/комментарий курьеру) отображается на
    // экране "Xarid qilish". Получателя матчим по номеру телефона из TestConfig, а не по
    // имени - имя владельца реального аккаунта не хранится в конфиге теста.
    public PaymentScreen verifyDeliveryDetailsDisplayed() {
        Assert.assertTrue(waitFor(DELIVERY_DETAILS_HEADER).isDisplayed(),
                "Заголовок \"Yetkazib berish tafsilotlari\" не отображается");
        Assert.assertTrue(waitFor(ADDRESS_ROW).isDisplayed(), "Адрес доставки не отображается");
        Assert.assertTrue(waitFor(ETA_ROW).isDisplayed(), "Ориентировочное время доставки не отображается");
        By recipientRow = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + TestConfig.phoneNumber() + "\")");
        Assert.assertTrue(waitFor(recipientRow).isDisplayed(), "Получатель (имя/телефон) не отображается");
        Assert.assertTrue(waitFor(COURIER_COMMENT_ROW).isDisplayed(), "Поле \"Kuryerga izoh qoldirish\" не отображается");
        return this;
    }

    // CHK-05: выбор "Naqd pul" отражается в строке способа оплаты на экране "Xarid
    // qilish" - до выбора там placeholder "To'lov usulini tanlang".
    public PaymentScreen verifyCashPaymentSelected() {
        Assert.assertTrue(waitFor(CASH_PAYMENT_SELECTED_INDICATOR).isDisplayed(),
                "Выбранный способ оплаты \"Naqd pul\" не отражается на экране чекаута");
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
