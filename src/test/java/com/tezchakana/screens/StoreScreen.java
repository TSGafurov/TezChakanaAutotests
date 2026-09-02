package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class StoreScreen extends BaseScreen {

    // Нижний зелёный бар с суммой/количеством ("1 490 so'm \n1 mahsulot"), появляется на
    // экране категории сразу после добавления товара - открывает мини-корзину.
    // descriptionContains("mahsulot") ловил не тот элемент: в сетках категорий, где
    // помимо самого бара есть заголовки секций вроде "Go'sht mahsulotlari"/"Sut
    // mahsulotlari" (тоже содержат подстроку "mahsulot"), тап уходил в заголовок раньше
    // бара по порядку в дереве - открывалась пустая корзина. descriptionMatches с "so'm"
    // перед "mahsulot" ловит именно бар с суммой. Символьный класс [\s\S] (нужен, т.к.
    // между "so'm" и "mahsulot" перенос строки, а "." его не матчит) на этом
    // UiSelector-парсере на устройстве не сработал ни разу (проверено напрямую через
    // curl к Appium REST) - похоже, экранирование "\\s"/"\\S" не долетает в нужном виде.
    // Инлайн-флаг "(?s)" (DOTALL) сработал сразу же - используем его вместо класса символов.
    private static final By CART_SUMMARY_BAR = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"(?s).*so'm.*mahsulot.*\")");

    public StoreScreen(AndroidDriver driver) {
        super(driver);
    }

    // STORE-02: подтверждает, что тап по категории реально открыл сетку товаров, а не
    // остался на месте (например, из-за не до конца завершившегося скролла, см. issue 6 в
    // exploration-notes.md) - не завязан на конкретный товар внутри категории. Пропажа
    // из дерева самой плитки категории (первая идея) оказалась ненадёжной: для коротких
    // названий ("Suv") тот же текст повторно используется как заголовок секции внутри
    // открывшейся сетки товаров и никогда не пропадает (проверено вживую 2026-08-29,
    // CheckoutFlowTest завис по таймауту, хотя переход фактически произошёл). Вместо
    // этого ждём появления любой карточки товара с ценой ("<сумма> so'm") - общий
    // признак сетки товаров для любой категории. Исключаем "mahsulot" в тексте, чтобы не
    // словить нижний бар суммы корзины (CART_SUMMARY_BAR), который тоже содержит "so'm"
    // и уже может быть виден, если в корзине что-то осталось с прошлого теста.
    private static final By PRODUCT_GRID_LOADED_INDICATOR = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\"(?s)(?!.*mahsulot).*so'm.*\")");

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
        waitFor(PRODUCT_GRID_LOADED_INDICATOR);
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

    // STORE-04: тот же карточный степпер, что и addProductToCart() выше ("+"/"-" внутри
    // карточки товара В СЕТКЕ категории, до открытия корзины) - "-" появляется на месте
    // "+", а сам "+" сдвигается влево, когда количество становится > 0 (карточка
    // превращается в полноширинный степпер "- N +"). Позиция "-" снята вживую 2026-08-29
    // с той же карточки ("Категория Вода.xml"-эквивалент для "Bozorlik"): фракция 0.504
    // по ширине карточки (в отличие от 0.883 у "+" - "-" не в самом левом углу степпера,
    // а ближе к его геометрическому центру, судя по снимку). Высота (0.098) та же, что у
    // "+" - оба симметричны по вертикали в одной полосе степпера.
    public StoreScreen decreaseQuantityOnCard(String productDescriptionContains) {
        By productCardLocator = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + productDescriptionContains + "\")");
        WebElement productCard = waitFor(productCardLocator);
        var cardRect = productCard.getRect();
        int tapX = cardRect.getX() + (int) Math.round(cardRect.getWidth() * 0.504);
        int tapY = cardRect.getY() + (int) Math.round(cardRect.getHeight() * 0.098);
        tapAt(tapX, tapY);
        return this;
    }

    // STORE-04: content-desc карточки - "<цена> so'm\n<название>" без количества, пока
    // товара нет в корзине, и "<цена> so'm\n<название>\n<N>" после первого "+" (см.
    // комментарий в addProductToCart() выше про формат количества).
    public String getProductCardText(String productDescriptionContains) {
        By productCardLocator = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + productDescriptionContains + "\")");
        return waitFor(productCardLocator).getAttribute("content-desc");
    }

    // Обнаружено 2026-08-29 при накопленном количестве товара (5x после нескольких
    // прогонов подряд без очистки корзины, noReset(true)): узел бара помечен
    // scrollable="true" в дереве доступности, и в этом состоянии Appium-`click()` по
    // найденному элементу перестаёт открывать шторку корзины (сама шторка при этом
    // открывается штатно по тому же координатному центру через обычный тап - проверено
    // вживую и через adb, и через `tapAt`). Тап по координатам центра элемента вместо
    // `.click()` - тот же обходной путь, что и везде в проекте для проблемных merged/
    // scrollable-узлов (см. addProductToCart() выше).
    public CartScreen openCartSummaryBar() {
        var rect = waitFor(CART_SUMMARY_BAR).getRect();
        tapAt(rect.getX() + rect.getWidth() / 2, rect.getY() + rect.getHeight() / 2);
        return new CartScreen(driver);
    }

    // STORE-03: содержимое бара ("<сумма> so'm \n<N> mahsulot") подтверждает, что тап
    // "+" реально долетел до бэкенда, а не просто провалился в пустоту - см. также
    // Known issue про нестабильное добавление в корзину в docs/exploration-notes.md.
    public String getCartSummaryBarText() {
        return waitFor(CART_SUMMARY_BAR).getAttribute("content-desc");
    }

    // Небольшая (не waitFor) проверка присутствия - нужна тестам, которым важно
    // абсолютное количество товаров в корзине, а не относительное изменение
    // (NoReset(true) делит реальную корзину между всеми тестами класса, см.
    // StoreAndCartTest.addingRecommendedItemUpdatesCartSummary).
    public boolean hasItemsInCart() {
        return !driver.findElements(CART_SUMMARY_BAR).isEmpty();
    }
}
