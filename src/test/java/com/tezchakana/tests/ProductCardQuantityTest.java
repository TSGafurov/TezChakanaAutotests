package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.StoreScreen;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * STORE-04 (см. docs/exploration-notes.md): повторный тап "+"/"-" на карточке товара В
 * СЕТКЕ категории (до открытия мини-корзины) меняет количество - не путать с CART-03
 * (количество уже в корзине, шторка "Savat", см. Known issue 16 - там пересчёт суммы
 * реально сломан). Каждый "+" использует ту же позицию карточки, что и
 * StoreScreen.addProductToCart(), "-" - зеркальную (StoreScreen.decreaseQuantityOnCard()) -
 * обе координаты сняты и проверены вживую 2026-08-29.
 */
public class ProductCardQuantityTest extends BaseTest {

    @Test(groups = "mutating")
    public void repeatedPlusMinusOnProductCardChangesQuantity() {
        StoreScreen storeScreen = new HomeScreen(driver)
                .openBazarTab()
                .openStore(TestConfig.storeName());
        storeScreen.scrollToCategory(TestConfig.groceryCategoryLabel());

        // Сравниваем относительно стартового количества, а не с абсолютным "1"/"2" -
        // реальный аккаунт может уже держать этот товар в корзине с прошлого прогона
        // (NoReset(true)), абсолютные значения были бы хрупкими.
        int before = quantityOf(storeScreen);

        storeScreen.addProductToCart(TestConfig.groceryProductName());
        int afterFirstPlus = quantityOf(storeScreen);
        Assert.assertEquals(afterFirstPlus, before + 1, "Первый \"+\" не увеличил количество на 1");

        storeScreen.addProductToCart(TestConfig.groceryProductName());
        int afterSecondPlus = quantityOf(storeScreen);
        Assert.assertEquals(afterSecondPlus, before + 2, "Второй \"+\" не увеличил количество на 1");

        storeScreen.decreaseQuantityOnCard(TestConfig.groceryProductName());
        int afterMinus = quantityOf(storeScreen);
        Assert.assertEquals(afterMinus, before + 1, "\"-\" не уменьшил количество на 1");
    }

    private int quantityOf(StoreScreen storeScreen) {
        String text = storeScreen.getProductCardText(TestConfig.groceryProductName());
        String[] lines = text.split("\n");
        String last = lines[lines.length - 1];
        try {
            return Integer.parseInt(last.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
