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
    // descriptionStartsWith: если комментарий уже введён, строка становится
    // "Kuryerga izoh qoldirish\n<текст>" и точный id её не находит (2026-10-02).
    private static final By COURIER_COMMENT_ROW = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Kuryerga izoh qoldirish\")");

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

    // ---------------- Подэкраны оформления (2026-10-02) ----------------
    //
    // У подэкранов ("Qabul qiluvchi ma'lumotlari", "Kuryerga izoh qoldirish",
    // "Promokodni kiriting", "Yetkazib berish vaqti") своя нижняя кнопка
    // ("Saqlash"/"Tasdiqlash") ровно на месте "Buyurtma qilish" экрана оформления. Если
    // подэкран не открылся, слепой tapBottomCta() оформил бы РЕАЛЬНЫЙ заказ (см.
    // project-accidental-order-placement-incident в памяти проекта). Поэтому любой тап по
    // нижней кнопке здесь - только через tapSubScreenCta(), которая сначала убеждается,
    // что открыт именно ожидаемый подэкран и что "Buyurtma qilish" на экране нет.

    // Строки блока доставки - дочерние элементы узла "Yetkazib berish tafsilotlari" в
    // фиксированном порядке: адрес, время, получатель, комментарий. Получателя ищем по
    // позиции, а не по номеру: номер меняет формат ("909023162" / "90 902 31 62") и может
    // быть пустым (BUG-002).
    private static final By RECIPIENT_ROW = AppiumBy.xpath(
            "//*[@content-desc='Yetkazib berish tafsilotlari']/*[@clickable='true'][3]");
    private static final By COMMENT_ROW = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Kuryerga izoh qoldirish\")");
    private static final By PROMO_ROW = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Promokod\")");
    private static final By TOTALS_BLOCK = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Umumiy qiymati\")");

    private static final By RECIPIENT_SCREEN = AppiumBy.accessibilityId("Qabul qiluvchi ma'lumotlari");
    private static final By COMMENT_SCREEN_TEXT = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"qo'shimcha ma'lumot kiriting\")");
    private static final By PROMO_SCREEN = AppiumBy.accessibilityId("Promokodni kiriting");
    private static final By TIME_SCREEN = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Yetkazib berish vaqti\")");
    private static final By TIME_SLOT_SOON = AppiumBy.accessibilityId("Yaqin 2 soat");
    private static final By TIME_SLOT_ANY = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"^(Bugun|Ertaga) [0-9]{2}:[0-9]{2} - [0-9]{2}:[0-9]{2}$\")");

    // Поля форм - по порядку EditText на подэкране (сверено вживую 2026-10-02).
    private static final By RECIPIENT_PHONE_FIELD = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(1)");
    private static final By COMMENT_TEXT_FIELD = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(0)");
    private static final By COMMENT_APARTMENT_FIELD = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(3)");
    private static final By PROMO_FIELD = AppiumBy.className("android.widget.EditText");

    // Крестик (закрыть) и стрелка (назад) подэкранов - icon-only, правый и левый верхний угол.
    private static final int SUB_SCREEN_CLOSE_REF_X = 1005;
    private static final int SUB_SCREEN_CLOSE_REF_Y = 163;
    private static final int SUB_SCREEN_BACK_REF_X = 75;
    private static final int SUB_SCREEN_BACK_REF_Y = 163;

    private void tapSubScreenCta(By expectedSubScreen) {
        waitFor(expectedSubScreen);
        if (!driver.findElements(PLACE_ORDER_BUTTON).isEmpty()) {
            throw new IllegalStateException("На экране видна \"Buyurtma qilish\" - тап по нижней кнопке мог бы оформить реальный заказ");
        }
        tapBottomCta();
    }

    private void waitForCheckout() {
        waitFor(PLACE_ORDER_BUTTON);
    }

    public boolean isCheckoutShown() {
        return !driver.findElements(PLACE_ORDER_BUTTON).isEmpty();
    }

    private void clearFocusedField(int maxChars) {
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.MOVE_END));
        for (int i = 0; i < maxChars; i++) {
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.DEL));
        }
    }

    // CHK-06 / BUG-002: сохранить получателя с номером phoneDigits (пусто - стереть номер).
    // Возвращает true, если приложение приняло такой номер и вернулось на экран оформления.
    public boolean saveRecipientPhone(String phoneDigits) {
        waitFor(RECIPIENT_ROW).click();
        waitFor(RECIPIENT_SCREEN);
        waitFor(RECIPIENT_PHONE_FIELD).click();
        sleep(java.time.Duration.ofMillis(500));
        clearFocusedField(16);
        if (!phoneDigits.isEmpty()) {
            typeViaAdb(phoneDigits);
        }
        sleep(java.time.Duration.ofMillis(700));
        driver.hideKeyboard();
        tapSubScreenCta(RECIPIENT_SCREEN);
        sleep(java.time.Duration.ofSeconds(2));
        boolean accepted = isCheckoutShown();
        if (!accepted) {
            // Номер отклонён - остаёмся на подэкране, выходим без сохранения.
            tapAt(scaledX(SUB_SCREEN_CLOSE_REF_X), scaledY(SUB_SCREEN_CLOSE_REF_Y));
            waitForCheckout();
        }
        return accepted;
    }

    public String recipientRowText() {
        return waitFor(RECIPIENT_ROW).getAttribute("content-desc");
    }

    // CHK-07: без обязательного "Xonadon" кнопка "Saqlash" комментария неактивна, с ним -
    // активна. Возвращает {активна без Xonadon, активна с Xonadon}. Ничего не сохраняет:
    // форма закрывается крестиком.
    public boolean[] commentSaveStateWithoutAndWithApartment(String comment, String apartment) {
        waitFor(COMMENT_ROW).click();
        waitFor(COMMENT_SCREEN_TEXT);
        waitFor(COMMENT_TEXT_FIELD).click();
        sleep(java.time.Duration.ofMillis(500));
        typeViaAdb(comment);
        sleep(java.time.Duration.ofMillis(500));
        driver.hideKeyboard();
        sleep(java.time.Duration.ofMillis(700));
        boolean withoutApartment = isBottomCtaActive();
        waitFor(COMMENT_APARTMENT_FIELD).click();
        sleep(java.time.Duration.ofMillis(500));
        typeViaAdb(apartment);
        sleep(java.time.Duration.ofMillis(500));
        driver.hideKeyboard();
        sleep(java.time.Duration.ofMillis(700));
        boolean withApartment = isBottomCtaActive();
        return new boolean[]{withoutApartment, withApartment};
    }

    // CHK-08 / BUG-007: закрыть форму комментария крестиком, без "Saqlash".
    public PaymentScreen closeCommentWithoutSaving() {
        waitFor(COMMENT_SCREEN_TEXT);
        tapAt(scaledX(SUB_SCREEN_CLOSE_REF_X), scaledY(SUB_SCREEN_CLOSE_REF_Y));
        waitForCheckout();
        return this;
    }

    // Комментарий курьеру сохраняется между оформлениями, а пустую форму сохранить
    // нельзя ("Saqlash" неактивна). Единственный найденный способ его сбросить - очистить
    // поля и закрыть крестиком: закрытие применяет то, что введено (BUG-007), в том числе
    // пустые поля. Если BUG-007 исправят, этот путь перестанет очищать комментарий -
    // поэтому метод возвращает, удалось ли, а тесты пишут предупреждение.
    private static final By COMMENT_ANY_FIELD = AppiumBy.className("android.widget.EditText");

    public boolean clearCourierComment() {
        if (commentRowText().equals("Kuryerga izoh qoldirish")) {
            return true;
        }
        waitFor(COMMENT_ROW).click();
        waitFor(COMMENT_SCREEN_TEXT);
        int fields = driver.findElements(COMMENT_ANY_FIELD).size();
        for (int i = 0; i < fields; i++) {
            By field = AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.EditText\").instance(" + i + ")");
            String value = waitFor(field).getText();
            if (value == null || value.isEmpty()) {
                continue;
            }
            waitFor(field).click();
            sleep(java.time.Duration.ofMillis(400));
            clearFocusedField(value.length() + 5);
        }
        driver.hideKeyboard();
        tapAt(scaledX(SUB_SCREEN_CLOSE_REF_X), scaledY(SUB_SCREEN_CLOSE_REF_Y));
        waitForCheckout();
        return commentRowText().equals("Kuryerga izoh qoldirish");
    }

    public String commentRowText() {
        return waitFor(COMMENT_ROW).getAttribute("content-desc");
    }

    // CHK-12: неверный промокод не применяется - строка промокода остаётся пустой,
    // итог не меняется. Текст ошибки - Flutter-toast вне дерева доступности, его язык
    // (BUG-008) автотестом не проверить, это остаётся ручной проверкой.
    public String totalsText() {
        return waitFor(TOTALS_BLOCK).getAttribute("content-desc");
    }

    public String promoRowText() {
        return waitFor(PROMO_ROW).getAttribute("content-desc");
    }

    public PaymentScreen submitPromoCode(String code) {
        waitFor(PROMO_ROW).click();
        waitFor(PROMO_SCREEN);
        waitFor(PROMO_FIELD).click();
        sleep(java.time.Duration.ofMillis(500));
        typeViaAdb(code);
        sleep(java.time.Duration.ofMillis(500));
        driver.hideKeyboard();
        tapSubScreenCta(PROMO_SCREEN);
        sleep(java.time.Duration.ofSeconds(4));
        if (!isCheckoutShown()) {
            tapAt(scaledX(SUB_SCREEN_BACK_REF_X), scaledY(SUB_SCREEN_BACK_REF_Y));
            waitForCheckout();
        }
        return this;
    }

    // CHK-04: экран выбора времени - "Yaqin 2 soat" плюс хотя бы один слот
    // "Bugun/Ertaga HH:MM - HH:MM". Выбор не меняем: выходим стрелкой назад.
    public PaymentScreen verifyDeliveryTimeOptionsShown() {
        waitFor(ETA_ROW).click();
        waitFor(TIME_SCREEN);
        Assert.assertTrue(waitFor(TIME_SLOT_SOON).isDisplayed(), "Нет варианта «Yaqin 2 soat»");
        Assert.assertFalse(driver.findElements(TIME_SLOT_ANY).isEmpty(),
                "Нет ни одного слота «Bugun/Ertaga HH:MM - HH:MM»");
        tapAt(scaledX(SUB_SCREEN_BACK_REF_X), scaledY(SUB_SCREEN_BACK_REF_Y));
        waitForCheckout();
        return this;
    }

    // 2026-09-11: до этого метода CheckoutFlowTest/CheckoutDetailsTest ничего не убирали
    // за собой после дохода до этого экрана - товар оставался в корзине, а сам экран
    // чекаута оставался открытым до конца всего прогона. HomeScreen.returnToHomeScreen()
    // не распознаёт этот экран (как и Cart) и не может с него восстановиться - на живом
    // прогоне testng.xml это уронило каскадом ВЕСЬ остаток сьюта (каждый следующий тест
    // падал по таймауту 15с). back() с этого экрана надёжно возвращает на CartScreen (тот
    // же паттерн, что CartScreen.close() уже использует для мини-корзины) - вызывающий
    // тест должен после этого явно очистить корзину через CartScreen.clearCart().
    public CartScreen goBackToCart() {
        driver.navigate().back();
        return new CartScreen(driver);
    }

    // Уборка после тестов CHK-*: вернуться в корзину и очистить её, чтобы товар и
    // открытый чекаут не ломали следующие тесты (см. SYS-13 в exploration-notes.md).
    public void leaveAndClearCart() {
        CartScreen cart = goBackToCart();
        cart.clearCart();
        cart.close();
    }
}
