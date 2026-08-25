package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.StoreScreen;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Навигация и корзина без оформления заказа: HOME-01, STORE-01/03, CART-01 (см.
 * docs/exploration-notes.md). Категория/товар берутся из TestConfig.groceryCategoryLabel()/
 * groceryProductName() - не из "Suv", т.к. тап по прокрученной плитке "Suv" сейчас
 * нестабилен (Known issue в exploration-notes.md).
 */
public class HomeAndCatalogTest extends BaseTest {

    @Test
    public void homeScreenLoadsWithChipsAndStoreList() {
        new HomeScreen(driver).verifyHomeLoaded();
    }

    @Test
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
}
