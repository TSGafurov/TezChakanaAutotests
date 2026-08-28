package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

import java.time.Duration;

/**
 * Деталь заказа (открывается из OrdersScreen.openFirstOrder()): статус-степпер (4
 * стадии) + красная иконка отмены -> диалог "Buyurtmani bekor qilish" (ORDH-03).
 * Проверено вживую 2026-08-28.
 */
public class OrderDetailsScreen extends BaseScreen {

    // Иконка-корзина отмены заказа (верх справа) - icon-only, без content-desc, но с
    // реальными (не на весь экран) bounds ([938,63][1080,207] на эталоне 1080x2400,
    // проверено вживую через page source) - тот же безопасный паттерн, что и у других
    // icon-only кнопок в проекте (см. HomeScreen.FAVORITES_ICON_REF).
    private static final int CANCEL_ICON_REF_X = 1009;
    private static final int CANCEL_ICON_REF_Y = 135;

    private static final By CANCEL_DIALOG_MESSAGE = AppiumBy.accessibilityId("Buyurtmani bekor qilmoqchimisiz?");

    // "Yo'q" - реальный (не смерженный) узел с отдельными bounds ([246,2214][325,2261]
    // на эталоне 1080x2400, проверено вживую), но clickable="false" в дереве
    // доступности - тот же случай, что и STARTUP_CONFIRM_ADDRESS_BUTTON в HomeScreen,
    // поэтому тап по координате центра, а не click() по элементу. Кнопка подтверждения
    // отмены ("Buyurtmani bekor qilish") в этом классе намеренно не заведена - тест
    // должен физически не иметь способа её нажать.
    private static final int CANCEL_DIALOG_NO_REF_X = 285;
    private static final int CANCEL_DIALOG_NO_REF_Y = 2238;

    public OrderDetailsScreen(AndroidDriver driver) {
        super(driver);
    }

    // ORDH-03: тап по иконке отмены открывает диалог с сообщением и полем причины -
    // само открытие диалога безопасно (ничего не отменяет). Закрывается через "Yo'q",
    // подтверждающая кнопка ("Buyurtmani bekor qilish") никогда не тапается.
    public OrderDetailsScreen verifyCancelDialogOpensAndDismiss() {
        tapAt(scaledX(CANCEL_ICON_REF_X), scaledY(CANCEL_ICON_REF_Y));
        Assert.assertTrue(waitFor(CANCEL_DIALOG_MESSAGE).isDisplayed(),
                "Диалог отмены заказа не открылся по иконке отмены");
        // Узел появляется в дереве доступности до того, как диалог долистает
        // анимацию появления снизу - тап по "Yo'q" сразу после waitFor() рискует
        // попасть по ещё не занявшей окончательное место кнопке (тот же паттерн,
        // что и в AddAddressScreen.confirmLocation()).
        sleep(Duration.ofMillis(500));
        tapAt(scaledX(CANCEL_DIALOG_NO_REF_X), scaledY(CANCEL_DIALOG_NO_REF_Y));
        try {
            waitUntilGone(CANCEL_DIALOG_MESSAGE);
        } catch (org.openqa.selenium.TimeoutException e) {
            Assert.fail("Диалог отмены заказа не закрылся после \"Yo'q\"");
        }
        return this;
    }
}
