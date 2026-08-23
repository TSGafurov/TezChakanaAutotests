package com.tezchakana.tests;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Checkout flow, locators taken from Appium Inspector XML page-source snapshots
 * (see /Users/timurgafurov/Desktop/TezChakanaXML). The app is a Flutter app whose
 * elements expose almost no resource-id/text - most locators are content-desc
 * (accessibility label) matches, sometimes merged across a whole screen.
 *
 * Known gap (see TODO marker): the login-prompt "Kirish" button is not clickable
 * in the accessibility tree, so it's tapped by coordinates as a fallback.
 */
public class CheckoutFlowTest extends BaseTest {

    private static final String PHONE_NUMBER_DIGITS = "909023162"; // без кода страны 998, он уже в UI
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);

    private static final By BAZAR_TAB = AppiumBy.accessibilityId("Bazar");

    // Метка магазина включает часы работы ("Ochiq\nEco Bazar\n08:00 - 19:00"),
    // поэтому матчим по частичному описанию, а не по точной строке.
    private static final By ECO_BAZAR_STORE_CARD =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Eco Bazar\").clickable(true)");

    // Плитка "Suv" внутри секции "Suv va ichimliklar" (см. "Scroll to water.xml").
    private static final By WATER_CATEGORY_TILE = AppiumBy.accessibilityId("Suv");

    private static final String HYDROLIFE_330ML_DESC = "1 490 so'm\nSuv Hydrolife gazsiz 330 ml";
    private static final By HYDROLIFE_PRODUCT_CARD = AppiumBy.accessibilityId(HYDROLIFE_330ML_DESC);

    // Нижний зелёный бар с суммой/количеством ("1 490 so'm \n1 mahsulot"), появляется
    // на экране категории сразу после добавления товара - открывает мини-корзину.
    private static final By CART_SUMMARY_BAR = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"mahsulot\")");

    // Мини-корзина (bottom sheet), открывается после тапа по CART_SUMMARY_BAR.
    private static final By PROCEED_TO_CHECKOUT = AppiumBy.accessibilityId("Savat\nTo'lovga o'tish");

    // Элементы "Kirish" на экране "Вход в профиль.xml" имеют clickable="false" в дереве
    // accessibility - рабочего локатора нет, поэтому тапаем по координатам центра нижней
    // кнопки "Kirish" (bounds [485,2193][595,2240] на экране 1080x2400 из снимка). Хрупко
    // на других разрешениях/плотностях экрана - заменить на нормальный локатор, как только
    // будет снимок с настоящим clickable-элементом.
    private static final int LOGIN_PROMPT_KIRISH_X = 540;
    private static final int LOGIN_PROMPT_KIRISH_Y = 2216;

    // Нижняя красная CTA-кнопка ("To'lovga o'tish" / "Davom etish" / "Tasdiqlash") тоже
    // смержена в один accessibility-узел на весь экран - click() тапает по ЦЕНТРУ экрана
    // (примерно y=1200), а не по самой кнопке внизу (~y=2184), поэтому промахивается.
    // Подтверждено вручную через adb: тап по (540,2184) стабильно попадает в кнопку.
    private static final int BOTTOM_CTA_X = 540;
    private static final int BOTTOM_CTA_Y = 2184;

    private static final By PHONE_INPUT_FIELD = AppiumBy.className("android.widget.EditText");
    private static final By CONTINUE_BUTTON = AppiumBy.accessibilityId("Telefon raqamini kiriting\nDavom etish");

    // Экран ввода OTP (см. "OTP.xml"): поле ввода кода + смерженная на весь экран
    // кнопка "Tasdiqlash" (Подтвердить).
    private static final By OTP_INPUT_FIELD = AppiumBy.className("android.widget.EditText");
    private static final By CONFIRM_OTP_BUTTON = AppiumBy.accessibilityId("Tasdiqlash");
    // Тестовый bypass-код, срабатывает на этом номере в тест-окружении.
    private static final String OTP_CODE = "000000";

    private static final By PAYMENT_METHOD_ROW = AppiumBy.accessibilityId("To'lov usuli\nTo'lov usulini tanlang");
    private static final By CASH_PAYMENT_OPTION = AppiumBy.accessibilityId("Naqd pul\nNaqd pul");
    private static final By PLACE_ORDER_BUTTON = AppiumBy.accessibilityId("Xarid qilish\nBuyurtma qilish");

    private static final By ORDER_SUCCESS_INDICATOR =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Buyurtma qabul qilindi\")");

    @Test
    public void completeCheckoutWithCashOnDelivery() {
        openBazarTab();
        openEcoBazarStore();
        scrollToWaterCategory();
        addHydrolifeWaterToCart();
        openCartSummaryBar();
        proceedToCheckoutFromMiniCart();
        confirmLoginPrompt();
        submitPhoneNumber();
        enterOtpCode();
        // Дальше пока не идём: подтверждение OTP ("Tasdiqlash") не вызывается, пока не
        // подтверждено, что тестовый bypass-код реально принимается бэкендом. Как только
        // будет добро - раскомментировать confirmOtpCode() и всё, что после него.
        // confirmOtpCode();
        // handleNotificationsPermission();
        // handleLocationPermission();
        // selectPaymentMethod();
        // placeOrder();
        // verifyOrderSuccess();
    }

    private void openBazarTab() {
        waitFor(BAZAR_TAB).click();
    }

    private void openEcoBazarStore() {
        waitFor(ECO_BAZAR_STORE_CARD).click();
    }

    private void scrollToWaterCategory() {
        List<WebElement> found = driver.findElements(WATER_CATEGORY_TILE);
        int maxSwipes = 10;
        while (found.isEmpty() && maxSwipes-- > 0) {
            swipeUpOnScreen();
            found = driver.findElements(WATER_CATEGORY_TILE);
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("Категория \"Suv\" не найдена после прокрутки списка категорий магазина");
        }
        found.get(0).click();
    }

    private void addHydrolifeWaterToCart() {
        WebElement productCard = waitFor(HYDROLIFE_PRODUCT_CARD);
        // Кнопка "+" не имеет своего content-desc/resource-id - относительный XPath от
        // найденного элемента ("./android.widget.ImageView[2]") не поддерживается
        // UiAutomator2 (NoSuchElementException), поэтому тапаем по координатам внутри
        // карточки товара. Смещение [473,63] от левого верхнего угла карточки взято из
        // снимка "Категория Вода.xml": карточка [0,1145][536,1788], кнопка [431,1166][515,1250].
        var cardRect = productCard.getRect();
        tapAt(cardRect.getX() + 473, cardRect.getY() + 63);
    }

    private void openCartSummaryBar() {
        waitFor(CART_SUMMARY_BAR).click();
    }

    private void proceedToCheckoutFromMiniCart() {
        waitFor(PROCEED_TO_CHECKOUT);
        tapAt(BOTTOM_CTA_X, BOTTOM_CTA_Y);
    }

    private void confirmLoginPrompt() {
        // Дожидаемся появления диалога (по некликабельному, но детектируемому узлу
        // "Kirish"), затем ждём завершения анимации bottom sheet перед тапом по
        // координатам - иначе тап попадает мимо настоящей позиции кнопки.
        By kirishNode = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Kirish\")");
        new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> !d.findElements(kirishNode).isEmpty());
        sleep(Duration.ofMillis(500));
        tapAt(LOGIN_PROMPT_KIRISH_X, LOGIN_PROMPT_KIRISH_Y);
    }

    private void submitPhoneNumber() {
        waitFor(PHONE_INPUT_FIELD).sendKeys(PHONE_NUMBER_DIGITS);
        waitFor(CONTINUE_BUTTON);
        tapAt(BOTTOM_CTA_X, BOTTOM_CTA_Y);
    }

    // Поле OTP - это 6-ячеечный pin-код виджет: Appium sendKeys() (ACTION_SET_TEXT) не
    // отображается в ячейках вообще, в отличие от `adb shell input text`, который
    // эмулирует реальные IME-события. Но даже так - между появлением EditText в дереве
    // (после отправки SMS, с сетевой задержкой) и реальной готовностью input connection
    // проходит время, поэтому ждём характерный заголовок экрана и делаем паузу перед
    // вводом (при ручной проверке ввод сразу после навигации не срабатывал). Поле уже
    // сфокусировано при открытии экрана - тап перед вводом не нужен и сбивает фокус.
    private void enterOtpCode() {
        By otpHeader = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"kodni kiriting\")");
        new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> !d.findElements(otpHeader).isEmpty());
        waitFor(OTP_INPUT_FIELD);
        sleep(Duration.ofSeconds(1));
        typeViaAdb(OTP_CODE);
    }

    private void confirmOtpCode() {
        waitFor(CONFIRM_OTP_BUTTON);
        tapAt(BOTTOM_CTA_X, BOTTOM_CTA_Y);
    }

    private void handleNotificationsPermission() {
        By allowButton = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button");
        clickIfPresent(allowButton, Duration.ofSeconds(10));
    }

    private void handleLocationPermission() {
        // Захваченный диалог предлагает только "While using the app" / "Only this time" /
        // "Don't allow" - варианта "Allow all the time" в этом снимке нет (см. TODO в шапке файла).
        By whileUsingApp = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_foreground_only_button");
        clickIfPresent(whileUsingApp, Duration.ofSeconds(10));
    }

    private void selectPaymentMethod() {
        waitFor(PAYMENT_METHOD_ROW).click();
        waitFor(CASH_PAYMENT_OPTION).click();
    }

    private void placeOrder() {
        // Тоже смерженная на весь экран кнопка - см. комментарий у BOTTOM_CTA_X/Y.
        waitFor(PLACE_ORDER_BUTTON);
        tapAt(BOTTOM_CTA_X, BOTTOM_CTA_Y);
    }

    private void verifyOrderSuccess() {
        WebElement successElement = waitFor(ORDER_SUCCESS_INDICATOR);
        org.testng.Assert.assertTrue(successElement.isDisplayed(), "Экран подтверждения заказа не отобразился");
    }

    private WebElement waitFor(By locator) {
        return new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> {
                    WebElement element = d.findElement(locator);
                    return element.isDisplayed() ? element : null;
                });
    }

    private boolean clickIfPresent(By locator, Duration timeout) {
        try {
            WebElement element = new WebDriverWait(driver, timeout)
                    .until(d -> {
                        List<WebElement> elements = d.findElements(locator);
                        return elements.isEmpty() ? null : elements.get(0);
                    });
            element.click();
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    private void typeViaAdb(String text) {
        try {
            new ProcessBuilder("adb", "shell", "input", "text", text).start().waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Не удалось ввести текст через adb: " + text, e);
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void tapAt(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofMillis(100)))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(tap));
    }

    private void swipeUpOnScreen() {
        Dimension size = driver.manage().window().getSize();
        int startX = size.width / 2;
        int startY = (int) (size.height * 0.8);
        int endY = (int) (size.height * 0.2);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofMillis(100)))
                .addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), startX, endY))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(swipe));
    }

    private void requireLocator(By locator, String fieldName) {
        if (locator == null) {
            throw new UnsupportedOperationException(
                    "Локатор " + fieldName + " ещё не задан - см. TODO в начале класса.");
        }
    }
}
