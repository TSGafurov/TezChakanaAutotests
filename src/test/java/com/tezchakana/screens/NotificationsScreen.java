package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

/**
 * "Xabarnomalar" (история пушей): NOTIF-01. Known issue 5 в exploration-notes.md
 * ("грузится ~8-10с, короткий таймаут даст ложный 'пусто'") опровергнуто владельцем
 * 2026-08-26; живая проверка 2026-09-01 подтвердила это ещё раз - экран на реальном
 * аккаунте загрузился за ~2с и показал непустую историю (карточки статусов заказов).
 * Стандартный {@link BaseScreen#WAIT_TIMEOUT} (15с) с запасом покрывает и
 * задокументированный ранее худший случай (8-10с).
 */
public class NotificationsScreen extends BaseScreen {

    private static final By SCREEN_TITLE = AppiumBy.accessibilityId("Xabarnomalar");

    // Каждая карточка - один смерженный узел с content-desc вида
    // "<заголовок>\n<текст>\n<дата> • <время>" - разделитель "•" присутствует во всех
    // карточках (подтверждено вживую, дамп дерева доступности 2026-09-01) и не
    // встречается больше нигде на этом экране (заголовок/кнопка "назад"/иконка
    // "отметить всё прочитанным" его не содержат), поэтому надёжно отличает карточку
    // от остального экрана без привязки к конкретному тексту статуса.
    private static final By NOTIFICATION_ITEM =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"•\")");

    public NotificationsScreen(AndroidDriver driver) {
        super(driver);
    }

    // NOTIF-01: экран грузится (заголовок появляется) и показывает историю пушей -
    // хотя бы одна карточка, а не ложное "пусто" от короткого таймаута.
    public NotificationsScreen verifyNotificationsShown() {
        Assert.assertTrue(waitFor(SCREEN_TITLE).isDisplayed(), "Заголовок \"Xabarnomalar\" не отображается");
        Assert.assertFalse(driver.findElements(NOTIFICATION_ITEM).isEmpty(),
                "Ни одной карточки уведомления не отображается в истории");
        return this;
    }
}
