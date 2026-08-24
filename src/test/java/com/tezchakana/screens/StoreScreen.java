package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class StoreScreen extends BaseScreen {

    // Нижний зелёный бар с суммой/количеством ("1 490 so'm \n1 mahsulot"), появляется на
    // экране категории сразу после добавления товара - открывает мини-корзину.
    private static final By CART_SUMMARY_BAR = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"mahsulot\")");

    public StoreScreen(AndroidDriver driver) {
        super(driver);
    }

    public StoreScreen scrollToCategory(String categoryLabel) {
        By categoryTile = AppiumBy.accessibilityId(categoryLabel);
        List<WebElement> found = driver.findElements(categoryTile);
        int maxSwipes = 10;
        while (found.isEmpty() && maxSwipes-- > 0) {
            swipeUpOnScreen();
            found = driver.findElements(categoryTile);
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("Категория \"" + categoryLabel + "\" не найдена после прокрутки списка категорий магазина");
        }
        found.get(0).click();
        return this;
    }

    // descriptionContains по уникальной подстроке без переноса строки (embedding a raw
    // newline внутрь selector-выражения UiAutomator рискованно - может сломать парсинг),
    // а не точный accessibility id: если товар уже есть в корзине, приложение дописывает к
    // content-desc количество ("...330 ml\n1"), и точное совпадение перестаёт находить
    // карточку.
    public StoreScreen addProductToCart(String productDescriptionContains) {
        By productCardLocator = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + productDescriptionContains + "\")");
        WebElement productCard = waitFor(productCardLocator);

        // Кнопка "+" не имеет своего content-desc/resource-id - относительный XPath от
        // найденного элемента ("./android.widget.ImageView[2]") не поддерживается
        // UiAutomator2 (NoSuchElementException), поэтому тапаем по координатам внутри
        // карточки товара. Позиция кнопки в снимке "Категория Вода.xml": карточка
        // [0,1145][536,1788], кнопка [431,1166][515,1250] - как доля ширины/высоты
        // карточки (0.883, 0.098), а не как смещение в чужих пикселях, чтобы не зависеть
        // от реального размера карточки на экране устройства.
        var cardRect = productCard.getRect();
        int tapX = cardRect.getX() + (int) Math.round(cardRect.getWidth() * 0.883);
        int tapY = cardRect.getY() + (int) Math.round(cardRect.getHeight() * 0.098);
        tapAt(tapX, tapY);
        return this;
    }

    public CartScreen openCartSummaryBar() {
        waitFor(CART_SUMMARY_BAR).click();
        return new CartScreen(driver);
    }
}
