package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.SkipException;

import java.util.List;

/**
 * Деталь заказа (открывается из OrdersScreen.openFirstOrder()). Редизайн v1.1.8
 * (проверено вживую 2026-09-29): вместо шапки с номером заказа и красной иконкой
 * отмены теперь - кнопка-стрелка "˅" слева, "Yordam" справа (открывает поддержку в
 * Telegram), крупный заголовок статуса ("Kuryer kechikmoqda", "Buyurtma yetkazildi"),
 * степпер, блоки курьера / адреса / "Buyurtmangiz" (со ссылкой "Ko'rish") / способа
 * оплаты и "Umumiy qiymati" с итогом "Jami". Номер заказа виден только на экране
 * состава заказа ("Ko'rish") - его заголовок.
 */
public class OrderDetailsScreen extends BaseScreen {

    private static final By ORDER_ITEMS_BLOCK = AppiumBy.accessibilityId("Buyurtmangiz");
    private static final By VIEW_ITEMS_LINK = AppiumBy.accessibilityId("Ko'rish");

    // Заголовок экрана состава заказа - сам номер ("TEZ00789"). ^/$ отличает его от
    // карточки списка заказов, где номер - лишь часть длинного content-desc. Символьный
    // класс "[0-9]" вместо "\d" - см. комментарий у StoreScreen.CART_SUMMARY_BAR про
    // экранированные regex-последовательности в androidUIAutomator.
    private static final By ITEMS_SCREEN_ORDER_NUMBER =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionMatches(\"^TEZ[0-9]+$\")");
    private static final By ITEMS_SCREEN_TOTAL_LABEL = AppiumBy.accessibilityId("Umumiy qiymati");

    // Любой элемент с "bekor" ("Buyurtmani bekor qilish" и т.п.) - в v1.1.8 для заказа
    // в статусе "в пути"/"курьер опаздывает" отмены на экране нет вовсе, а для
    // статуса, где она есть, новая раскладка ещё не снята вживую.
    private static final By CANCEL_CONTROL =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"bekor\")");

    // Стрелка "˅" (детали заказа) и "←" (состав заказа) - обе icon-only без
    // content-desc в левом верхнем углу, в пределах той же зоны, что и
    // HomeScreen.APPBAR_BACK_ARROW_REF_X/Y (проверено вживую: [42,84][147,189] на эталоне).
    private static final int TOP_LEFT_BUTTON_REF_X = 73;
    private static final int TOP_LEFT_BUTTON_REF_Y = 135;

    private final String listCardText;

    public OrderDetailsScreen(AndroidDriver driver, String listCardText) {
        super(driver);
        this.listCardText = listCardText;
    }

    // ORDH-03/ORDH-09: в v1.1.8 кнопка отмены есть не у каждого заказа (у "в пути"
    // её нет, см. FAQ "Buyurtmani bekor qilish mumkinmi?"). Сам диалог отмены после
    // редизайна ещё не снят вживую, поэтому тест честно пропускается, а не тапает
    // вслепую по старым координатам (прежняя координата иконки теперь попадает в
    // "Yordam").
    public OrderDetailsScreen verifyCancelDialogOpensAndDismiss() {
        waitFor(ORDER_ITEMS_BLOCK);
        if (driver.findElements(CANCEL_CONTROL).isEmpty()) {
            close();
            throw new SkipException("У первого заказа нет кнопки отмены (статус: " + statusOf(listCardText)
                    + "). Нужен заказ на ранней стадии, чтобы снять новый диалог отмены v1.1.8.");
        }
        close();
        throw new SkipException("Кнопка отмены найдена, но диалог отмены v1.1.8 ещё не снят вживую - "
                + "дописать шаги по живому снимку, прежде чем тапать.");
    }

    // ORDH-02: номер, статус и сумма согласованы между карточкой списка заказов,
    // экраном деталей и экраном состава заказа ("Ko'rish").
    public OrderDetailsScreen verifyOrderDetailsShowConsistentInfo() {
        waitFor(ORDER_ITEMS_BLOCK);
        Assert.assertFalse(statusOf(listCardText).isEmpty(), "Статус заказа пуст в карточке списка: " + listCardText);

        waitFor(VIEW_ITEMS_LINK).click();
        String itemsTitle = waitFor(ITEMS_SCREEN_ORDER_NUMBER).getAttribute("content-desc");
        Assert.assertEquals(itemsTitle, numberOf(listCardText),
                "Номер заказа на экране состава не совпадает с карточкой списка: " + listCardText);

        String listSum = digitsOf(sumOf(listCardText));
        String itemsTotal = digitsOf(textRightOf(ITEMS_SCREEN_TOTAL_LABEL));
        Assert.assertEquals(itemsTotal, listSum,
                "Сумма в составе заказа не совпадает с суммой в списке: " + listCardText);

        tapAt(scaledX(TOP_LEFT_BUTTON_REF_X), scaledY(TOP_LEFT_BUTTON_REF_Y));
        waitFor(ORDER_ITEMS_BLOCK);
        close();
        return this;
    }

    // Стрелка "˅" закрывает детали и возвращает на список заказов ("Buyurtmalar").
    public void close() {
        tapAt(scaledX(TOP_LEFT_BUTTON_REF_X), scaledY(TOP_LEFT_BUTTON_REF_Y));
    }

    // Значение в той же строке, что и подпись (например "Umumiy qiymati" → "2 700 so'm"):
    // Flutter отдаёт подпись и значение отдельными узлами без общего родителя с
    // content-desc, поэтому берём узел с "so'm", ближайший по вертикали к подписи.
    private String textRightOf(By label) {
        WebElement labelNode = waitFor(label);
        int labelCenterY = labelNode.getRect().getY() + labelNode.getRect().getHeight() / 2;
        List<WebElement> amounts = driver.findElements(
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"so'm\")"));
        WebElement closest = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WebElement amount : amounts) {
            int centerY = amount.getRect().getY() + amount.getRect().getHeight() / 2;
            int distance = Math.abs(centerY - labelCenterY);
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = amount;
            }
        }
        Assert.assertNotNull(closest, "Не найдено значение суммы рядом с подписью " + label);
        return closest.getAttribute("content-desc");
    }

    // Карточка списка: "Buyurtma raqami:\nTEZ00789\nYetkazib berilmoqda\nBuyurtma sanasi:\n28.09.2026\n2 700 uzs"
    private static String numberOf(String cardText) {
        return line(cardText, 1);
    }

    private static String statusOf(String cardText) {
        return line(cardText, 2);
    }

    private static String sumOf(String cardText) {
        String[] lines = cardText.split("\n");
        return lines[lines.length - 1];
    }

    private static String line(String cardText, int index) {
        String[] lines = cardText.split("\n");
        return lines.length > index ? lines[index].trim() : "";
    }

    private static String digitsOf(String text) {
        return text.replaceAll("[^0-9]", "");
    }
}
