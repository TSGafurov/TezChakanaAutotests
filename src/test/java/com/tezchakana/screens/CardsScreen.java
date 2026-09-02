package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

/**
 * "Mening kartalarim" (сохранённые карты): CARD-01. Known issue 2 в
 * exploration-notes.md ("Xatolik ro'y berdi" без кнопки retry) опровергнуто
 * владельцем 2026-08-26 - на актуальном билде экран грузится нормально, подтверждено
 * вживую 2026-08-28 (2 реальные карты на аккаунте).
 */
public class CardsScreen extends BaseScreen {

    private static final By SCREEN_TITLE = AppiumBy.accessibilityId("Mening kartalarim");

    // Строка карты - "<сумма> so'm\n<банк/Unknown> • <последние цифры>". Матчим по
    // "so'm" вместо конкретной суммы/номера - баланс реального аккаунта меняется со
    // временем, тест не должен зависеть от текущих цифр (и не должен их печатать в
    // сообщениях об ошибке - это реальные финансовые данные).
    private static final By CARD_ROW =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"so'm\")");

    private static final By ADD_CARD_BUTTON = AppiumBy.accessibilityId("Kartani qo'shish");

    public CardsScreen(AndroidDriver driver) {
        super(driver);
    }

    // CARD-01: экран грузится (без "Xatolik ro'y berdi") и показывает список
    // сохранённых карт, плюс кнопку добавления карты - она видна независимо от того,
    // есть ли уже карты.
    //
    // 2026-09-02, воспроизведено вживую дважды подряд: заголовок экрана - статичный
    // элемент шапки, отрисовывается сразу, а сами карты (с балансом) подгружаются
    // отдельным сетевым запросом чуть позже - мгновенный driver.findElements() сразу
    // после появления заголовка (без ожидания) иногда успевал сработать до того, как
    // карты долетели, и ложно решал, что карт нет, хотя на аккаунте их 2 (подтверждено
    // снимком дерева доступности в тот же момент через appium-mcp). waitFor() вместо
    // мгновенной проверки - тот же паттерн, что и везде в проекте для списков с
    // отдельной подгрузкой контента (см. PROF-A02 в docs/exploration-notes.md).
    public CardsScreen verifyCardsShown() {
        Assert.assertTrue(waitFor(SCREEN_TITLE).isDisplayed(), "Заголовок \"Mening kartalarim\" не отображается");
        Assert.assertTrue(waitFor(CARD_ROW).isDisplayed(), "Ни одной карты не отображается в списке");
        Assert.assertTrue(driver.findElement(ADD_CARD_BUTTON).isDisplayed(),
                "Кнопка \"Kartani qo'shish\" не отображается");
        return this;
    }
}
