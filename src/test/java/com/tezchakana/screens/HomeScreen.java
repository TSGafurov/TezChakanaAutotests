package com.tezchakana.screens;

import com.tezchakana.config.TestConfig;
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

    // Кнопка "Manzil qo'shish" внутри шторки выбора адреса - визуально отдельная
    // (серая, между карточкой текущего адреса и красной "Tasdiqlash"), но в дереве
    // доступности слита в тот же узел ADDRESS_PICKER_SHEET целиком (в отличие от той же
    // кнопки в полной сетке "Manzillar" в профиле, где она отдельный элемент - см.
    // AddressesScreen). Тап по координате, снято вживую 2026-08-28 на эталоне 1080x2400.
    private static final int ADDRESS_PICKER_ADD_ADDRESS_REF_X = 540;
    private static final int ADDRESS_PICKER_ADD_ADDRESS_REF_Y = 2085;

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

    // "Location Accuracy" - системный диалог Google Play services (пакет
    // com.google.android.gms, НЕ часть тестируемого приложения), который может
    // перекрыть весь экран после выдачи разрешения на геолокацию ("While using the
    // app" в PermissionsScreen.handleLocationPermission()). Обнаружен 2026-08-27: без
    // этой проверки returnToHomeScreen() тапал по координатам Home-вкладки вслепую под
    // диалогом другого пакета все 5 попыток и не находил "Bazar", из-за чего упали все
    // тесты в прогоне подряд (ProfileAuthorizedTest/DetailsTest/AddressesTest) - не
    // баг локаторов, а необработанный сторонний системный диалог. "No thanks"
    // безопасен - отклоняет только автонастройку точности геолокации, не отзывает уже
    // выданное разрешение.
    private static final By LOCATION_ACCURACY_DIALOG_DISMISS =
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"No thanks\")");

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

    // Нижняя вкладка профиля - без content-desc/resource-id, тот же паттерн, что и
    // HOME_TAB_REF (bounds на эмуляторе 1080x2400: [859,2203][1038,2319], центр
    // x=948/y=2261).
    private static final int PROFILE_TAB_REF_X = 948;
    private static final int PROFILE_TAB_REF_Y = 2261;

    public HomeScreen(AndroidDriver driver) {
        super(driver);
    }

    public BazarScreen openBazarTab() {
        returnToHomeScreen();
        waitFor(BAZAR_TAB).click();
        return new BazarScreen(driver);
    }

    // PROF-G01/G02: вкладка профиля доступна с любого верхнеуровневого экрана (Home,
    // Bazar), returnToHomeScreen() здесь не для перехода на конкретный экран, а чтобы
    // гарантированно уйти со вложенных экранов (магазин/товар), где нижней навигации
    // не видно.
    public ProfileScreen openProfileTab() {
        returnToHomeScreen();
        tapAt(scaledX(PROFILE_TAB_REF_X), scaledY(PROFILE_TAB_REF_Y));
        return new ProfileScreen(driver);
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

    // HOME-02: тап по чипу-фильтру (не "Hammasi") реально меняет список магазинов, а не
    // только визуально переключает сам чип - см. Known issue 3 ("возможный баг
    // фильтрации категорий") в exploration-notes.md: под Kafe/Gullar изначально
    // показывались те же магазины, что и под "Hammasi". Опровергнуто владельцем
    // 2026-08-26 после множества попыток вручную, но до сих пор не было
    // автоматизированного регресс-теста на этот случай - HOME-03 проверял только что
    // "Hammasi" возвращает полный список после другого чипа, не саму фильтрацию.
    public HomeScreen verifyChipFiltersStoreList() {
        returnToHomeScreen();
        waitFor(HAMMASI_CHIP).click();
        Assert.assertTrue(waitFor(STORE_LIST_HEADER).isDisplayed(), "Список магазинов не отображается под \"Hammasi\"");
        Set<String> hammasiStores = visibleStoreCardTexts();
        Assert.assertFalse(hammasiStores.isEmpty(), "Список магазинов под \"Hammasi\" пуст - сравнивать не с чем");

        waitFor(KAFE_CHIP).click();
        // Не ждём STORE_LIST_HEADER здесь намеренно - под "Kafe" список теоретически
        // может быть пустым (это тоже валидное доказательство фильтрации, а не только
        // непустой отличающийся список), в отличие от заведомо непустого "Hammasi" выше.
        sleep(Duration.ofMillis(500));
        Set<String> kafeStores = visibleStoreCardTexts();
        Assert.assertNotEquals(kafeStores, hammasiStores,
                "Список магазинов под \"Kafe\" совпадает со списком под \"Hammasi\" - фильтрация не работает (регрессия issue 3)");
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

    // Открывает флоу добавления нового адреса через карту с поиском (см.
    // AddAddressScreen/AddressFormScreen) - в отличие от selectSavedAddress() ниже, не
    // зависит от того, что нужный адрес уже сохранён в "Manzillar" и что его текст
    // совпадает с конфигом (address.alternate=Yunusobod, например, не совпадает ни с
    // одним реально сохранённым адресом на 2026-08-28). Проверено вживую 2026-08-28:
    // после AddressFormScreen.save() приложение возвращается сюда же, на Home, с новым
    // адресом уже активным.
    public AddAddressScreen openAddAddressScreen() {
        openAddressPickerFromHeader();
        tapAt(scaledX(ADDRESS_PICKER_ADD_ADDRESS_REF_X), scaledY(ADDRESS_PICKER_ADD_ADDRESS_REF_Y));
        return new AddAddressScreen(driver);
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

    // Заведения показываются только для адреса с реальным покрытием доставки - и
    // список не пуст для адреса с покрытием (проверка ниже), и он ПУСТ вместе с
    // сообщением об отсутствии доставки для адреса без покрытия (verifyNoStoresShown).
    public HomeScreen verifyStoresShown() {
        Assert.assertTrue(waitFor(STORE_LIST_HEADER).isDisplayed(),
                "Список \"Yaqin atrofdagi do'konlar\" не отображается для адреса с покрытием доставки");
        Assert.assertFalse(driver.findElements(STORE_CARD).isEmpty(),
                "Ни одной карточки магазина не отображается для адреса с покрытием доставки");
        return this;
    }

    public HomeScreen verifyNoStoresShown() {
        verifyNoDeliveryCoverageMessageShown();
        Assert.assertTrue(driver.findElements(STORE_CARD).isEmpty(),
                "Отображаются карточки магазинов, хотя адрес вне зоны доставки");
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
    //
    // 2026-08-27, воспроизведено вживую через appium-mcp: если предыдущий тест оставил
    // приложение на экране с формой (DetailsScreen "Mening tafsilotlarim",
    // AddressesScreen "Manzilni o'zgartirish" и т.п.), у которого кнопка "Saqlash"
    // смержена на ВЕСЬ экран (bounds [0,0][ширина,высота], clickable=true независимо от
    // того, редактировалась форма или нет - см. DetailsScreen про DET-02), координатный
    // тап по Home-вкладке ниже попадает не в навигацию, а в этот "Saqlash" - 5 попыток
    // подряд реально тапали по нему, не находя "Bazar" (тот же класс риска, что и
    // project-accidental-address-duplication-incident в памяти проекта, где то же самое
    // "Saqlash" на экране добавления адреса стало причиной 10 дублей). Полей не
    // менялось, поэтому в этот раз обошлось без порчи данных, но полагаться на это
    // нельзя. Пока виден такой узел - уходим аппаратным back (тот же безопасный паттерн,
    // что уже применяется в AddressesScreen.verifyTappingAddressOpensEditScreen()),
    // вместо тапа по координате.
    private static final By DANGEROUS_FULLSCREEN_SAVE_CTA = AppiumBy.accessibilityId("Saqlash");

    // 2026-08-28, воспроизведено вживую на "Manzillar" (список адресов): координата
    // Home-вкладки ниже физически совпадает с полноширинной кнопкой "Manzil qo'shish"
    // на этом экране - слепой тап туда вместо навигации на Home открыл флоу добавления
    // адреса (с картой и предзаполненной точкой, готовой к "Tasdiqlash"). Тот же класс
    // риска, что и DANGEROUS_FULLSCREEN_SAVE_CTA выше - см.
    // project_accidental_address_duplication_incident в памяти проекта.
    private static final By DANGEROUS_FULLSCREEN_ADD_ADDRESS_CTA = AppiumBy.accessibilityId("Manzil qo'shish");

    // 2026-08-28, воспроизведено вживую, несколько итераций подряд:
    //
    // 1) Первая версия тапала по координате Home-вкладки повторно (как и в
    //    исходном коде) - "сворачивает текущий стек экранов по одному уровню за
    //    тап". Один раз это привело к попаданию по опасной CTA ("Manzil qo'shish"
    //    на "Manzillar") - добавлен блок-лист опасных CTA (см. ниже), тап по
    //    координате запрещён, пока такая CTA видна.
    // 2) Вторая версия ограничила тап РОВНО одним разом за весь цикл, дальше
    //    полагаясь на back() - но back() в этом приложении на многих экранах
    //    (Sozlamalar, Profile-root, OrderDetailsScreen) не поднимается на один
    //    уровень назад, а закрывает всё приложение целиком (то в Google Lens, то
    //    на лаунчер устройства) - воспроизведено многократно с отладочным логом:
    //    ACTIVATE → BACK → уход в Lens → ACTIVATE → BACK → снова уход в Lens,
    //    пока не кончались попытки. Единственного тапа по OrderDetailsScreen
    //    тоже не хватало - see, комментарий (1), тап снимает по одному уровню за
    //    раз, а не сразу до Home.
    //
    // Итог: тап по координате - основная стратегия для ЛЮБОГО нераспознанного
    // экрана (как в исходном коде), не ограниченная одной попыткой за цикл.
    // back() используется ТОЛЬКО когда на экране видна известная опасная CTA
    // (тап по ней сам по себе рискован) - тогда back с неё, по опыту, работает
    // безопасно. Диалог подтверждения адреса и уход из пакета приложения
    // (Lens/лаунчер) обрабатываются отдельно, до общей развилки тап/back.
    private void returnToHomeScreen() {
        clickIfPresent(LOCATION_ACCURACY_DIALOG_DISMISS, Duration.ofMillis(500));
        int attempt = 0;
        int maxAttempts = 8;
        while (driver.findElements(BAZAR_TAB).isEmpty() && attempt++ < maxAttempts) {
            boolean dangerousCtaVisible = !driver.findElements(DANGEROUS_FULLSCREEN_SAVE_CTA).isEmpty()
                    || !driver.findElements(DANGEROUS_FULLSCREEN_ADD_ADDRESS_CTA).isEmpty();
            if (!TestConfig.appPackage().equals(driver.getCurrentPackage())) {
                driver.activateApp(TestConfig.appPackage());
            } else if (!driver.findElements(STARTUP_ADDRESS_DIALOG).isEmpty()) {
                tapAt(scaledX(STARTUP_CONFIRM_ADDRESS_REF_X), scaledY(STARTUP_CONFIRM_ADDRESS_REF_Y));
            } else if (dangerousCtaVisible) {
                driver.navigate().back();
            } else {
                tapAt(scaledX(HOME_TAB_REF_X), scaledY(HOME_TAB_REF_Y));
            }
            sleep(Duration.ofMillis(700));
        }
    }
}
