package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

public class AddressesScreen extends BaseScreen {

    // "Manzillar" - проверено live 2026-08-27 на реальном аккаунте, 3 сохранённых адреса
    // (см. project-real-account-live-backend в памяти проекта).
    private static final By ADDRESS_ITEM = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Boshlang‘ich manzil\")");
    private static final By ADD_ADDRESS_BUTTON = AppiumBy.accessibilityId("Manzil qo'shish");

    // ADDR-03 (см. verifyTappingAddressOpensEditScreen): экран редактирования адреса -
    // содержит кнопку удаления (корзина) и "Saqlash". НЕ используются здесь - только
    // проверяем, что экран открылся, и уходим назад без изменений.
    private static final By EDIT_ADDRESS_SCREEN_TITLE = AppiumBy.accessibilityId("Manzilni o'zgartirish");

    public AddressesScreen(AndroidDriver driver) {
        super(driver);
    }

    // ADDR-01: список сохранённых адресов отображается.
    public AddressesScreen verifyAddressListShown() {
        Assert.assertTrue(waitFor(ADDRESS_ITEM).isDisplayed(), "Список адресов не отображается");
        Assert.assertTrue(driver.findElement(ADD_ADDRESS_BUTTON).isDisplayed(), "\"Manzil qo'shish\" не отображается");
        return this;
    }

    // ADDR-03 (уточнено 2026-08-27): вопреки формулировке в docs/exploration-notes.md
    // ("выбор адреса меняет адрес доставки"), тап по карточке сохранённого адреса в
    // списке "Manzillar" на самом деле открывает экран РЕДАКТИРОВАНИЯ этого адреса
    // ("Manzilni o'zgartirish" с кнопкой удаления и "Saqlash"), а не делает его активным
    // адресом доставки напрямую - живой механизм выбора активного адреса нашёлся позже,
    // 2026-08-28, но с другой стороны: он не тап по уже сохранённой карточке, а флоу
    // добавления нового адреса через карту с поиском (см. HomeScreen.openAddAddressScreen()
    // / AddAddressScreen / AddressFormScreen). Тест проверяет фактическое поведение
    // именно тапа по карточке, не тапая ни корзину, ни "Saqlash" на реальном сохранённом
    // адресе.
    public AddressesScreen verifyTappingAddressOpensEditScreen() {
        waitFor(ADDRESS_ITEM).click();
        Assert.assertTrue(waitFor(EDIT_ADDRESS_SCREEN_TITLE).isDisplayed(),
                "Тап по адресу не открыл экран \"Manzilni o'zgartirish\"");
        driver.navigate().back();
        Assert.assertTrue(waitFor(ADDRESS_ITEM).isDisplayed(), "Список адресов не восстановился после возврата");
        return this;
    }

    // Открывает на редактирование САМЫЙ ПЕРВЫЙ адрес в сетке "Manzillar" - новые адреса
    // всегда появляются первыми (проверено вживую 2026-08-28: и после добавления Chust,
    // и после добавления Tashkent новая карточка была первой в списке, а не последней),
    // поэтому этого достаточно, чтобы найти и удалить только что добавленный адрес, не
    // завися от его текста (гео-подсказка на карте может не совпадать с введённым
    // запросом - см. AddAddressScreen.FIRST_SEARCH_RESULT).
    public EditAddressScreen openFirstAddressForEditing() {
        waitFor(ADDRESS_ITEM).click();
        return new EditAddressScreen(driver);
    }
}
