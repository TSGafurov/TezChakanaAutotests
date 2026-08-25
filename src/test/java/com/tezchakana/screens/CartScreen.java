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
    // "O'chirish" (подтвердить очистку) - смерженный узел clickable=false с реальными
    // (не на весь экран) bounds, тап по координатам центра, как и с другими такими
    // узлами в приложении.
    private static final int CLEAR_CART_CONFIRM_REF_X = 794;
    private static final int CLEAR_CART_CONFIRM_REF_Y = 2237;

    // "+" рядом с товаром в мини-корзине - без content-desc, только координаты (сняты
    // со снимка карточки товара на экране 1080x2400: [912,427][996,511]). Метод
    // предполагает единственную позицию в корзине, как и остальные CART-тесты.
    private static final int QUANTITY_PLUS_REF_X = 954;
    private static final int QUANTITY_PLUS_REF_Y = 469;

    private static final By ADD_TO_CART_ON_PRODUCT_SCREEN = AppiumBy.accessibilityId("Savatga qo'shish");

    // Первая карточка блока рекомендаций "Hech narsani unutmadingizmi?" - позиция в
    // шторке фиксирована, а конкретный товар в ней меняется от сессии к сессии
    // (алгоритмическая рекомендация), поэтому тап по координатам, а не по названию.
    private static final int RECOMMENDED_ITEM_REF_X = 312;
    private static final int RECOMMENDED_ITEM_REF_Y = 1537;

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
        tapAt(scaledX(CLEAR_CART_CONFIRM_REF_X), scaledY(CLEAR_CART_CONFIRM_REF_Y));
        Assert.assertTrue(waitFor(EMPTY_CART_MESSAGE).isDisplayed(), "Корзина не опустела после \"Tozalash\"");
    }

    public String getProductRowText(String productDescriptionContains) {
        By productRow = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + productDescriptionContains + "\")");
        return waitFor(productRow).getAttribute("content-desc");
    }

    // CART-03: "+" пересчитывает "Umumiy qiymati" - сравнение до/после делает вызывающий
    // тест через getTotalText().
    public CartScreen increaseQuantity() {
        tapAt(scaledX(QUANTITY_PLUS_REF_X), scaledY(QUANTITY_PLUS_REF_Y));
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
        tapAt(scaledX(RECOMMENDED_ITEM_REF_X), scaledY(RECOMMENDED_ITEM_REF_Y));
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
