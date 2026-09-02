package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.CartScreen;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.StoreScreen;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Магазин и корзина без оформления заказа: STORE-01/03, CART-01/02/03/04 (см.
 * docs/exploration-notes.md). Категория/товар берутся из TestConfig.groceryCategoryLabel()/
 * groceryProductName() - не из "Suv", т.к. тап по прокрученной плитке "Suv" сейчас
 * нестабилен (Known issue в exploration-notes.md).
 *
 * Раньше эти тесты и Home/onboarding-тесты жили в одном классе HomeAndCatalogTest
 * (10 @Test) - разделены 2026-08-25, см. HomeAndOnboardingTest и
 * [[project-session-churn-flakiness]] в памяти проекта про причину разделения.
 */
public class StoreAndCartTest extends BaseTest {

    @Test(groups = "mutating")
    public void addingProductUpdatesCartSummaryAndMiniCart() {
        StoreScreen storeScreen = new HomeScreen(driver)
                .openBazarTab()
                .openStore(TestConfig.storeName());

        // STORE-01: если бы категории магазина не подгрузились, scrollToCategory ниже
        // упал бы с IllegalStateException, не найдя плитку категории.
        storeScreen.scrollToCategory(TestConfig.groceryCategoryLabel());

        storeScreen.addProductToCart(TestConfig.groceryProductName());

        String summaryBarText = storeScreen.getCartSummaryBarText();
        Assert.assertTrue(summaryBarText.contains("mahsulot"),
                "Бар корзины не показывает количество товаров после добавления: " + summaryBarText);

        storeScreen.openCartSummaryBar()
                .verifyMiniCartContents(TestConfig.storeName(), TestConfig.groceryProductName())
                .close();
    }

    @Test(groups = "mutating")
    public void clearCartEmptiesIt() {
        StoreScreen storeScreen = new HomeScreen(driver)
                .openBazarTab()
                .openStore(TestConfig.storeName());

        storeScreen.scrollToCategory(TestConfig.groceryCategoryLabel());
        storeScreen.addProductToCart(TestConfig.groceryProductName());

        storeScreen.openCartSummaryBar().clearCart();
    }

    @Test(groups = "mutating")
    public void increasingQuantityRecalculatesTotal() {
        StoreScreen storeScreen = new HomeScreen(driver)
                .openBazarTab()
                .openStore(TestConfig.storeName());

        storeScreen.scrollToCategory(TestConfig.groceryCategoryLabel());
        storeScreen.addProductToCart(TestConfig.groceryProductName());

        CartScreen cartScreen = storeScreen.openCartSummaryBar();
        String totalBefore = cartScreen.getTotalText();
        cartScreen.increaseQuantity(TestConfig.groceryProductName());
        String totalAfter = cartScreen.getTotalText();

        Assert.assertNotEquals(totalAfter, totalBefore,
                "Итоговая сумма не изменилась после увеличения количества: " + totalBefore);
        cartScreen.close();
    }

    @Test(groups = "mutating")
    public void addingRecommendedItemUpdatesCartSummary() {
        StoreScreen storeScreen = new HomeScreen(driver)
                .openBazarTab()
                .openStore(TestConfig.storeName());

        storeScreen.scrollToCategory(TestConfig.groceryCategoryLabel());
        storeScreen.addProductToCart(TestConfig.groceryProductName());

        CartScreen cartScreen = storeScreen.openCartSummaryBar();
        cartScreen.addFirstRecommendedItem();
        cartScreen.close();

        String summaryBarText = storeScreen.getCartSummaryBarText();
        Assert.assertTrue(summaryBarText.contains("2 mahsulot"),
                "Бар корзины не показывает 2 товара после добавления рекомендованного: " + summaryBarText);
    }
}
