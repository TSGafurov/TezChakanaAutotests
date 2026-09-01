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

    // ORDH-02: заголовок экрана - отдельный смерженный узел ровно с номером заказа
    // ("TEZ00168", без переноса строк и доп. текста) - descriptionMatches с якорями
    // ^/$ отличает его от карточки сводки заказа ниже (OrderCard в OrdersScreen.
    // ORDER_CARD и её собственная копия здесь - тот же формат "Buyurtma raqami:
    // ...\n<номер>\n<статус>\n..."), у которой номер заказа - лишь часть куда более
    // длинного content-desc. Номер заказа динамический (разный для каждого заказа
    // реального аккаунта), поэтому не хардкодим конкретное значение.
    // Символьный класс "[0-9]" вместо "\d" - см. комментарий у StoreScreen.CART_SUMMARY_BAR
    // про то, что экранированные regex-последовательности (\s/\S) не доживают до
    // реального движка через этот androidUIAutomator-парсер на устройстве, "\d" по той
    // же причине воспроизведено вживую 2026-08-31 (не матчил вообще ничего, хотя
    // непереэкранированный класс символов сработал сразу).
    private static final By TITLE_ORDER_NUMBER =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionMatches(\"^TEZ[0-9]+$\")");

    // Тот же формат карточки, что и OrdersScreen.ORDER_CARD в списке заказов - здесь
    // это карточка КОНКРЕТНОГО открытого заказа внизу экрана деталей.
    private static final By ORDER_INFO_CARD =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Buyurtma raqami:\")");

    // Степпер статуса (4 стадии: check/корзина/карта/флаг) - живьём подтверждено
    // 2026-08-31, что все 4 иконки степпера - android.widget.ImageView без
    // content-desc, неотличимые друг от друга через дерево доступности (нет сигнала,
    // какая стадия активна, кроме визуального цвета фона, не читаемого через
    // Appium). Из-за этого сам степпер НЕ проверяется отдельным ассертом - см.
    // ORDH-02 в docs/exploration-notes.md про то, почему это ограничение
    // accessibility-дерева приложения, а не пробел в тесте.

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

    // ORDH-02: номер заказа в шапке экрана совпадает с номером в карточке сводки того
    // же заказа, статус - не пустая метка, сумма - в ожидаемом формате ("<число> uzs").
    // Не проверяет сам статус-степпер - см. комментарий у ORDER_INFO_CARD выше.
    public OrderDetailsScreen verifyOrderDetailsShowConsistentInfo() {
        String titleOrderNumber = waitFor(TITLE_ORDER_NUMBER).getAttribute("content-desc");
        String infoCardText = waitFor(ORDER_INFO_CARD).getAttribute("content-desc");

        Assert.assertTrue(infoCardText.contains(titleOrderNumber),
                "Номер заказа в шапке (\"" + titleOrderNumber + "\") не совпадает с карточкой деталей: " + infoCardText);
        Assert.assertTrue(infoCardText.matches("(?s).*\\d[\\d\\s]*uzs.*"),
                "Сумма заказа не отображается в ожидаемом формате (\"<число> uzs\"): " + infoCardText);

        String[] lines = infoCardText.split("\n");
        Assert.assertTrue(lines.length >= 3, "Карточка деталей заказа не содержит ожидаемых строк (номер/статус/дата/сумма): " + infoCardText);
        String status = lines[2].trim();
        Assert.assertFalse(status.isEmpty(), "Статус заказа пуст в карточке деталей: " + infoCardText);
        return this;
    }
}
