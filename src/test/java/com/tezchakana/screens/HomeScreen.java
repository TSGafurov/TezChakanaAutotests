package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class HomeScreen extends BaseScreen {

    private static final By BAZAR_TAB = AppiumBy.accessibilityId("Bazar");
    private static final By HAMMASI_CHIP = AppiumBy.accessibilityId("Hammasi");
    private static final By GULLAR_CHIP = AppiumBy.accessibilityId("Gullar");
    private static final By KAFE_CHIP = AppiumBy.accessibilityId("Kafe");
    private static final By QANDOLAT_CHIP = AppiumBy.accessibilityId("Qandolat");

    // Заголовок списка магазинов содержит специфичный апостроф (do‘konlar), матчим по
    // "atrofdagi", чтобы не зависеть от того, какой именно символ апострофа use'ится.
    private static final By STORE_LIST_HEADER =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"atrofdagi\")");

    // Карточки магазинов в списке начинаются с "Ochiq\n<название>..." - используем как
    // общий локатор для сравнения видимого набора карточек до/после скролла (HOME-07).
    private static final By STORE_CARD =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Ochiq\")");

    // Шапка "Manzil\n<текущий адрес>" - единственный элемент на Home с "Manzil" в
    // content-desc (не путать с "Boshlang'ich manzil" внутри шторки списка адресов,
    // регистр другой и они не видны одновременно).
    private static final By ADDRESS_HEADER =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Manzil\").clickable(true)");

    // Шторка выбора адреса - один и тот же смерженный CTA "Manzil\nManzil
    // qo'shish\nTasdiqlash" на весь экран, открывается и с шапки Home, и с кнопки
    // "Manzilni o'zgartirish" на стартовом диалоге (см. verifyChangingAddressOpensPicker).
    private static final By ADDRESS_PICKER_SHEET = AppiumBy.accessibilityId("Manzil\nManzil qo'shish\nTasdiqlash");

    // Сообщение вместо списка магазинов, если для выбранного адреса нет покрытия
    // доставки - надёжный сигнал того, что смена адреса реально повлияла на Home
    // (HOME-05), в отличие от сверки конкретных названий магазинов, которые и сами по
    // себе меняются при скролле (см. HOME-07).
    private static final By NO_DELIVERY_COVERAGE_MESSAGE =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"yetkazib bermaymiz\")");

    // Диалог подтверждения адреса при (пере)запуске приложения - воспроизведён не
    // только на самом первом запуске, а при каждом полном перезапуске процесса
    // приложения (см. Known issue в exploration-notes.md, исправляет более раннее
    // предположение "только первый запуск"). "Ha, men shu yerdaman" - смерженный узел
    // clickable=false с реальными (не на весь экран) bounds - тап по координатам
    // центра, как и с другими такими узлами в приложении.
    private static final By STARTUP_ADDRESS_DIALOG = AppiumBy.accessibilityId("Shu manzilga buyurtma berilsinmi?");
    private static final By STARTUP_CONFIRM_ADDRESS_BUTTON = AppiumBy.accessibilityId("Ha, men shu yerdaman");
    private static final int STARTUP_CONFIRM_ADDRESS_REF_X = 540;
    private static final int STARTUP_CONFIRM_ADDRESS_REF_Y = 2069;
    private static final By STARTUP_CHANGE_ADDRESS_BUTTON = AppiumBy.accessibilityId("Manzilni o'zgartirish");

    // Нижняя вкладка "Bosh sahifa" (иконка домика) - без content-desc/resource-id (не
    // находится ни через accessibility id, ни через uiautomator dump на этом экране),
    // поэтому тап по координатам. Позиция снята со снимка 1080x2400.
    // Y скорректирован 2026-08-25: исходное значение 2313 било ниже реальных bounds
    // иконки (проверено uiautomator dump на эмуляторе 1080x2400: [42,2224][221,2298],
    // центр y=2261) - из-за этого returnToHomeScreen() иногда не долетал до Home за
    // отведённые 5 тапов.
    private static final int HOME_TAB_REF_X = 150;
    private static final int HOME_TAB_REF_Y = 2261;

    // Иконка избранного (сердце), верх справа - без content-desc/resource-id, только
    // координаты (снято со снимка 1080x2400, bounds [912,63][1080,231]).
    private static final int FAVORITES_ICON_REF_X = 996;
    private static final int FAVORITES_ICON_REF_Y = 147;

    public HomeScreen(AndroidDriver driver) {
        super(driver);
    }

    public BazarScreen openBazarTab() {
        returnToHomeScreen();
        waitFor(BAZAR_TAB).click();
        return new BazarScreen(driver);
    }

    // HOME-01: баннер не проверяем - это картинка без content-desc в дереве
    // доступности (карусель Flutter-виджетов без semantics-меток), стабильного
    // локатора для него нет.
    public HomeScreen verifyHomeLoaded() {
        returnToHomeScreen();
        Assert.assertTrue(waitFor(HAMMASI_CHIP).isDisplayed(), "Чип \"Hammasi\" не отображается на Home");
        Assert.assertTrue(driver.findElement(BAZAR_TAB).isDisplayed(), "Чип \"Bazar\" не отображается на Home");
        Assert.assertTrue(driver.findElement(GULLAR_CHIP).isDisplayed(), "Чип \"Gullar\" не отображается на Home");
        Assert.assertTrue(driver.findElement(KAFE_CHIP).isDisplayed(), "Чип \"Kafe\" не отображается на Home");
        Assert.assertTrue(driver.findElement(QANDOLAT_CHIP).isDisplayed(), "Чип \"Qandolat\" не отображается на Home");
        Assert.assertTrue(waitFor(STORE_LIST_HEADER).isDisplayed(), "Список \"Yaqin atrofdagi do'konlar\" не отображается на Home");
        return this;
    }

    // HOME-03: тап по чипу-фильтру, затем "Hammasi" не ломает экран и возвращает полный
    // список - саму фильтрацию по чипам не проверяем (см. Known issue про её
    // недостоверность в exploration-notes.md), только что "Hammasi" всегда безопасно
    // отрабатывает после другого чипа.
    public HomeScreen verifyHammasiRendersListAfterAnotherChip() {
        returnToHomeScreen();
        waitFor(GULLAR_CHIP).click();
        waitFor(HAMMASI_CHIP).click();
        Assert.assertTrue(waitFor(STORE_LIST_HEADER).isDisplayed(),
                "Список магазинов не отображается после \"Hammasi\"");
        Assert.assertFalse(driver.findElements(STORE_CARD).isEmpty(),
                "Ни одной карточки магазина не осталось после \"Hammasi\"");
        return this;
    }

    // HOME-06: иконка избранного открывает "Sevimlilar" (список избранных товаров).
    // Возвращаемся на Home аппаратной кнопкой back - см. комментарий в CartScreen.close()
    // про то, почему тап по координатам ненадёжен для закрытия таких экранов.
    public HomeScreen verifyFavoritesIconOpensFavoritesScreen() {
        returnToHomeScreen();
        tapAt(scaledX(FAVORITES_ICON_REF_X), scaledY(FAVORITES_ICON_REF_Y));
        Assert.assertTrue(waitFor(AppiumBy.accessibilityId("Sevimlilar")).isDisplayed(),
                "Экран \"Sevimlilar\" не открылся по иконке избранного");
        driver.navigate().back();
        return this;
    }

    // HOME-07: скролл списка магазинов открывает карточки, которых не было видно
    // изначально - сверяем множество видимых карточек до/после, а не рост общего счётчика
    // (карточки выше скрываются при скролле, поэтому счётчик сам по себе не растёт).
    public HomeScreen verifyStoreListScrollRevealsMoreStores() {
        returnToHomeScreen();
        Set<String> before = visibleStoreCardTexts();
        swipeUpOnScreen();
        Set<String> after = visibleStoreCardTexts();
        after.removeAll(before);
        Assert.assertFalse(after.isEmpty(), "После скролла не появилось ни одной новой карточки магазина");
        return this;
    }

    private Set<String> visibleStoreCardTexts() {
        Set<String> texts = new HashSet<>();
        for (WebElement element : driver.findElements(STORE_CARD)) {
            texts.add(element.getAttribute("content-desc"));
        }
        return texts;
    }

    // HOME-05: смена адреса доставки влияет на список ближайших магазинов. Открывает
    // ту же шторку выбора адреса, что и ONB-04 (см. verifyChangingAddressOpensPicker) -
    // с шапки Home, а не со стартового диалога.
    public String getCurrentAddressText() {
        returnToHomeScreen();
        return waitFor(ADDRESS_HEADER).getAttribute("content-desc");
    }

    public HomeScreen openAddressPickerFromHeader() {
        returnToHomeScreen();
        waitFor(ADDRESS_HEADER).click();
        waitFor(ADDRESS_PICKER_SHEET);
        return this;
    }

    // Тап по строке адреса выбирает его, но не подтверждает выбор сам по себе (сверено
    // вживую) - нужен ещё тап по нижнему CTA "Tasdiqlash", тот же смерженный на весь
    // экран паттерн, что и везде в приложении (см. BaseScreen.tapBottomCta).
    public HomeScreen selectSavedAddress(String addressDescriptionContains) {
        By addressRow = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + addressDescriptionContains + "\")");
        waitFor(addressRow).click();
        tapBottomCta();
        return this;
    }

    public HomeScreen verifyNoDeliveryCoverageMessageShown() {
        Assert.assertTrue(waitFor(NO_DELIVERY_COVERAGE_MESSAGE).isDisplayed(),
                "Сообщение об отсутствии доставки по адресу не отобразилось");
        return this;
    }

    // ONB-04: диалог подтверждения адреса на (пере)запуске приложения - вызывается сразу
    // после activate() в тесте, ДО returnToHomeScreen() (диалог перекрывает Home, а
    // returnToHomeScreen() вслепую тапает по домику, не находя "Bazar" под диалогом).
    public HomeScreen verifyStartupAddressDialogShown() {
        Assert.assertTrue(waitFor(STARTUP_ADDRESS_DIALOG).isDisplayed(),
                "Диалог подтверждения адреса не появился при (пере)запуске приложения");
        return this;
    }

    public HomeScreen confirmStartupAddress() {
        tapAt(scaledX(STARTUP_CONFIRM_ADDRESS_REF_X), scaledY(STARTUP_CONFIRM_ADDRESS_REF_Y));
        return this;
    }

    // ONB-04: "Manzilni o'zgartirish" на стартовом диалоге открывает тот же список
    // адресов, что и шапка Home (см. openAddressPickerFromHeader). Закрываем аппаратным
    // back - один тап уводит и со шторки, и с самого стартового диалога сразу на Home
    // (сверено вживую), без смены адреса.
    public HomeScreen verifyChangeAddressOpensPickerFromStartupDialog() {
        waitFor(STARTUP_CHANGE_ADDRESS_BUTTON).click();
        Assert.assertTrue(waitFor(ADDRESS_PICKER_SHEET).isDisplayed(),
                "Список адресов не открылся по \"Manzilni o'zgartirish\" со стартового диалога");
        driver.navigate().back();
        return this;
    }

    // noReset(true) сохраняет данные приложения, но НЕ сбрасывает in-app навигацию -
    // новая Appium-сессия просто подхватывается к тому экрану, на котором предыдущая
    // сессия/разведка оставила приложение (может быть где угодно, не обязательно Home).
    // Тап по нижней вкладке "Home" не прыгает сразу на глобальный Home, а сворачивает
    // текущий стек экранов по одному уровню за тап (подтверждено вручную: из категории
    // товаров первый тап приводит на корень магазина, только следующий - на Home),
    // поэтому тапаем повторно, пока не появится вкладка "Bazar".
    private void returnToHomeScreen() {
        int maxTaps = 5;
        while (driver.findElements(BAZAR_TAB).isEmpty() && maxTaps-- > 0) {
            tapAt(scaledX(HOME_TAB_REF_X), scaledY(HOME_TAB_REF_Y));
            sleep(Duration.ofMillis(700));
        }
    }
}
