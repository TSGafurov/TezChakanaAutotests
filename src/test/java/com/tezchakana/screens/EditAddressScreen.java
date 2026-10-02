package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * "Manzilni o'zgartirish" - экран редактирования сохранённого адреса, открывается тапом
 * по карточке в "Manzillar" (см. AddressesScreen). Помимо формы с "Saqlash" (тем же
 * смерженным на весь экран CTA, что и в AddressFormScreen - см. её комментарий про
 * инцидент с дублированием) содержит отдельную кнопку удаления адреса.
 */
public class EditAddressScreen extends BaseScreen {

    // Иконка-корзина удаления адреса - без content-desc/resource-id (icon-only), но
    // проверено вживую 2026-08-28 через page source: у неё РЕАЛЬНЫЕ, не смерженные на
    // весь экран bounds ([42,2180][158,2295] на эталоне 1080x2400, отдельно от
    // "Saqlash"), поэтому тап по координате её центра безопасен - тот же паттерн, что и
    // у других icon-only кнопок в проекте (см. HomeScreen.HOME_TAB_REF/FAVORITES_ICON_REF).
    private static final int DELETE_ICON_REF_X = 100;
    private static final int DELETE_ICON_REF_Y = 2238;

    private static final By DELETE_CONFIRM_MESSAGE = AppiumBy.accessibilityId("Manzilni o'chirmoqchimisiz?");
    private static final By DELETE_CONFIRM_BUTTON = AppiumBy.accessibilityId("O'chirish");

    // ADDR-07 / BUG-004: поле "Manzil belgisi" открывает шторку со списком меток. В
    // v1.1.8 (проверено вживую 2026-09-29) вместо списка там пустое состояние "So'rov
    // bo'yicha hech qanday natija topilmadi" - поэтому все адреса называются
    // "Boshlang‘ich manzil". Признак открытой шторки - её смерженный CTA "Tasdiqlash".
    private static final By EDIT_SCREEN_TITLE = AppiumBy.accessibilityId("Manzilni o'zgartirish");
    // Текст "Manzil belgisi" есть у двух узлов: подписи над полем и самого выпадающего
    // поля. Нужен кликабельный - подпись тап игнорирует (2026-10-02).
    private static final By LABEL_FIELD = AppiumBy.xpath(
            "//*[@content-desc='Manzil belgisi' and @clickable='true']");
    private static final By LABEL_SHEET = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Tasdiqlash\")");
    private static final By LABEL_LIST_EMPTY = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"natija topilmadi\")");

    public EditAddressScreen(AndroidDriver driver) {
        super(driver);
    }

    // Удаляет адрес после подтверждения в диалоге. "O'chirish" - реальный, не
    // смерженный узел (bounds [711,2214][877,2261], проверено вживую 2026-08-28 через
    // page source перед тем, как полагаться на тап - см. project-accidental-order-placement-incident
    // и project-accidental-address-duplication-incident в памяти проекта про риск
    // слепых тапов рядом со смерженными CTA в этом приложении), поэтому тапается по
    // accessibility id, а не по координате.
    // Ничего не сохраняет: шторку и экран редактирования закрываем аппаратным back
    // (на экране есть смерженный "Saqlash" на весь экран - тапы по координатам опасны).
    public boolean isAddressLabelListEmpty() {
        waitFor(EDIT_SCREEN_TITLE);
        waitFor(LABEL_FIELD).click();
        waitFor(LABEL_SHEET);
        sleep(java.time.Duration.ofSeconds(2));
        boolean empty = !driver.findElements(LABEL_LIST_EMPTY).isEmpty();
        driver.navigate().back();
        waitFor(EDIT_SCREEN_TITLE);
        driver.navigate().back();
        return empty;
    }

    public AddressesScreen delete() {
        tapAt(scaledX(DELETE_ICON_REF_X), scaledY(DELETE_ICON_REF_Y));
        waitFor(DELETE_CONFIRM_MESSAGE);
        waitFor(DELETE_CONFIRM_BUTTON).click();
        return new AddressesScreen(driver);
    }
}
