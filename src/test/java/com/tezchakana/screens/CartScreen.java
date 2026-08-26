package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

public class CartScreen extends BaseScreen {

    // Мини-корзина (bottom sheet), открывается после тапа по нижнему бару суммы.
    private static final By PROCEED_TO_CHECKOUT = AppiumBy.accessibilityId("Savat\nTo'lovga o'tish");

    // Итоговый блок ("Keling, hisoblaymiz\nMahsulotlar (N)\n...\nUmumiy qiymati\n...") -
    // один смерженный узел без разбивки на подэлементы, как и остальные текстовые блоки
    // в этом приложении - см. комментарий в BaseScreen.
    private static final By TOTAL_BLOCK = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Umumiy qiymati\")");

    private static final By CLEAR_CART_BUTTON = AppiumBy.accessibilityId("Tozalash");
    private static final By CLEAR_CART_CONFIRM_MESSAGE =
            AppiumBy.accessibilityId("Savatingizdagi barcha mahsulotlarni o'chirib tashlamoqchimisiz?");
    private static final By EMPTY_CART_MESSAGE =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"bo'sh ko'rinadi\")");
    // "O'chirish" (подтвердить очистку) имеет свой content-desc, отличный от "Bekor
    // qilish" (отмена) - раньше здесь был тап по координатам центра, что было
    // ненадёжно только из-за отсутствия проверки; сам узел прекрасно находится по
    // accessibility id (проверено live 2026-08-26), несмотря на clickable=false в
    // дереве - это тот же паттерн, что и у кнопки подтверждения отмены заказа в
    // ORDH-05.
    private static final By CLEAR_CART_CONFIRM_BUTTON = AppiumBy.accessibilityId("O'chirish");

    private static final By ADD_TO_CART_ON_PRODUCT_SCREEN = AppiumBy.accessibilityId("Savatga qo'shish");

    // Блок рекомендаций в мини-корзине - конкретный товар в первой карточке меняется
    // от сессии к сессии (алгоритмическая рекомендация), но сама карточка - обычный
    // clickable-узел с собственным content-desc внутри секции "Hech narsani
    // unutmadingizmi?" (проверено live 2026-08-26 - раньше ошибочно считалось, что
    // карточка совсем без content-desc). XPath ищет первый clickable-потомок этой
    // секции, а не координату - если секция с рекомендациями не отрисовалась,
    // waitFor() упадёт по таймауту вместо тапа мимо, в смерженный на весь экран
    // "Savat\nTo'lovga o'tish" (см. инцидент 2026-08-25,
    // project-accidental-order-placement-incident в памяти проекта).
    private static final By FIRST_RECOMMENDED_ITEM = AppiumBy.xpath(
            "//android.view.View[@content-desc=\"Hech narsani unutmadingizmi?\"]"
                    + "//android.view.View[@clickable=\"true\"][1]");

    public CartScreen(AndroidDriver driver) {
        super(driver);
    }

    // CART-01: магазин, товар и итоговая сумма отображаются в мини-корзине.
    public CartScreen verifyMiniCartContents(String expectedStoreName, String expectedProductDescriptionContains) {
        Assert.assertTrue(waitFor(AppiumBy.accessibilityId(expectedStoreName)).isDisplayed(),
                "Название магазина \"" + expectedStoreName + "\" не отображается в мини-корзине");
        By productRow = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + expectedProductDescriptionContains + "\")");
        Assert.assertTrue(waitFor(productRow).isDisplayed(),
                "Товар \"" + expectedProductDescriptionContains + "\" не отображается в мини-корзине");
        Assert.assertTrue(waitFor(TOTAL_BLOCK).isDisplayed(), "Итоговая сумма не отображается в мини-корзине");
        return this;
    }

    // CART-02: "Tozalash" очищает корзину после диалога подтверждения. После очистки
    // приложение само уводит с мини-корзины на пустой экран вкладки "Savat", поэтому
    // метод не возвращает CartScreen.
    public void clearCart() {
        waitFor(CLEAR_CART_BUTTON).click();
        waitFor(CLEAR_CART_CONFIRM_MESSAGE);
        waitFor(CLEAR_CART_CONFIRM_BUTTON).click();
        Assert.assertTrue(waitFor(EMPTY_CART_MESSAGE).isDisplayed(), "Корзина не опустела после \"Tozalash\"");
    }

    public String getProductRowText(String productDescriptionContains) {
        By productRow = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + productDescriptionContains + "\")");
        return waitFor(productRow).getAttribute("content-desc");
    }

    // CART-03: "+" пересчитывает "Umumiy qiymati" - сравнение до/после делает вызывающий
    // тест через getTotalText(). Кнопка "+" без своего content-desc - второй
    // clickable ImageView в строке товара (первый - "-"), поэтому находим её
    // относительно строки товара по названию, а не по координатам (проверено live
    // 2026-08-26) - если строка товара или её второй clickable-потомок не найдутся,
    // упадёт с понятной ошибкой вместо тапа мимо.
    public CartScreen increaseQuantity(String productDescriptionContains) {
        By quantityPlusButton = AppiumBy.xpath(
                "(//android.view.View[contains(@content-desc, \"" + productDescriptionContains + "\")]"
                        + "/android.widget.ImageView[@clickable=\"true\"])[2]");
        waitFor(quantityPlusButton).click();
        return this;
    }

    public String getTotalText() {
        return waitFor(TOTAL_BLOCK).getAttribute("content-desc");
    }

    // CART-04: тап по рекомендованному товару открывает экран товара, а не добавляет
    // его сразу - нужен ещё тап "Savatga qo'shish" там. Итоговая сумма мини-корзины не
    // обновляется мгновенно после возврата назад (визуальный лаг шторки) - проверять
    // сумму нужно через StoreScreen.getCartSummaryBarText() на закрытой шторке (см.
    // close()), а не сразу здесь.
    public CartScreen addFirstRecommendedItem() {
        waitFor(FIRST_RECOMMENDED_ITEM).click();
        waitFor(ADD_TO_CART_ON_PRODUCT_SCREEN).click();
        driver.navigate().back();
        return this;
    }

    public LoginScreen proceedToCheckout() {
        waitFor(PROCEED_TO_CHECKOUT);
        tapBottomCta();
        return new LoginScreen(driver);
    }

    // Мини-корзина занимает весь экран смерженным кликабельным узлом "Savat\nTo'lovga
    // o'tish" (см. PROCEED_TO_CHECKOUT) - если оставить её открытой в конце теста,
    // следующий тест может случайно "проехать" в реальный чекаут через координатный тап
    // HomeScreen.returnToHomeScreen() (воспроизведено 2026-08-25: тап по координате
    // домика с открытой мини-корзиной уводил на экран "Xarid qilish" вместо сворачивания
    // навигации). Аппаратная кнопка back закрывает шторку надёжно, в отличие от тапа по
    // координатам, поэтому тесты, открывающие мини-корзину, должны закрывать её явно.
    public void close() {
        driver.navigate().back();
    }
}
