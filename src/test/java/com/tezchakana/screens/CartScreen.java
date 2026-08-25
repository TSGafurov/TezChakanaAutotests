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
