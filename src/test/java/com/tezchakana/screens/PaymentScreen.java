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

    // ORD-02: экран ошибки оплаты ("Xato\nTo'lovda xatolik yuz berdi\ndio_error:
    // connection_error\nBosh sahifa\nQayta urinib ko'ring") - тот же смерженный на весь
    // экран паттерн, что и остальные диалоги/CTA в приложении. Захвачено вживую
    // 2026-08-29 (`ScratchExploreOrd02`, network отключалась через adb прямо перед
    // placeOrder(), см. Known issue 1 в docs/exploration-notes.md) - дамп дерева
    // показал, что "Bosh sahifa" и "Qayta urinib ko'ring" НЕ имеют своих отдельных
    // кликабельных узлов (только родительский full-screen non-clickable контейнер и
    // отдельная кнопка "X" закрытия с bounds [933,89][1080,236]) - обе кнопки доступны
    // только тапом по координатам, как и везде в этом приложении для такого паттерна.
    private static final By ORDER_ERROR_INDICATOR =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"To'lovda xatolik yuz berdi\")");
    private static final int ORDER_ERROR_GO_HOME_REF_X = 283;
    private static final int ORDER_ERROR_GO_HOME_REF_Y = 2136;
    private static final int ORDER_ERROR_RETRY_REF_X = 796;
    private static final int ORDER_ERROR_RETRY_REF_Y = 2136;

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

    // ORD-02: проверяет полноэкранный error-state после неудачной попытки оформления
    // заказа (см. ORDER_ERROR_INDICATOR выше).
    public PaymentScreen verifyOrderErrorShown() {
        Assert.assertTrue(waitFor(ORDER_ERROR_INDICATOR).isDisplayed(),
                "Экран ошибки оплаты (\"To'lovda xatolik yuz berdi\") не отобразился");
        return this;
    }

    public HomeScreen goHomeFromOrderError() {
        tapAt(scaledX(ORDER_ERROR_GO_HOME_REF_X), scaledY(ORDER_ERROR_GO_HOME_REF_Y));
        return new HomeScreen(driver);
    }

    // ORD-04: с отключённой сетью повторная попытка проваливается с ТЕМ ЖЕ самым
    // текстом ошибки, что и исходная - визуально неотличимо от "кнопка вообще ничего не
    // делает". Проверено вживую 2026-08-29 двумя разными способами, и оба ничего не
    // доказывают: (1) content-desc узла ошибки не исчезает даже на мгновение между
    // попытками; (2) WebElement, снятый ДО тапа, не становится stale после - но для
    // Flutter это ожидаемо для ЛЮБОГО setState-ребилда (element/semantics-дерево
    // намеренно сохраняет identity неизменных поддеревьев), а не признак того, что
    // ничего не произошло. Технически надёжного способа доказать, что тап запустил
    // именно НОВЫЙ сетевой запрос, а не просто пришёлся мимо, в этом приложении нет -
    // здесь только тап по координате и возврат на этот же экран; caller (см. ORD-04 в
    // exploration-notes.md) сам проверяет, что экран ошибки после тапа по-прежнему в
    // штатном, не сломанном состоянии.
    public PaymentScreen retryOrderFromError() {
        tapAt(scaledX(ORDER_ERROR_RETRY_REF_X), scaledY(ORDER_ERROR_RETRY_REF_Y));
        return this;
    }
}
