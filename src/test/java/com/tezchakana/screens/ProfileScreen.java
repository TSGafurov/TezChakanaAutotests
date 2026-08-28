package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

public class ProfileScreen extends BaseScreen {

    // Гостевой профиль: заглушка аватара (картинка без content-desc, не проверяем),
    // "Kirish" и три карточки-ссылки - все проверены live на реальном аккаунте
    // 2026-08-27 в разлогиненном состоянии.
    private static final By GUEST_KIRISH_BUTTON = AppiumBy.accessibilityId("Kirish");
    private static final By GUEST_FAVORITES_CARD =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Sevimlilar\")");
    private static final By HELP_CARD = AppiumBy.accessibilityId("Yordam");
    private static final By SETTINGS_CARD = AppiumBy.accessibilityId("Sozlamalar");

    // Экран входа по номеру телефона - тот же экран, что открывается из промпта логина
    // при оформлении заказа (см. LoginScreen). verifyKirishOpensLoginFlow() ниже
    // намеренно останавливается здесь, не вводя номер - PROF-G02 проверяет только сам
    // переход в флоу логина. Полный логин (номер → OTP → "Tasdiqlash") с
    // тестовым/bypass-кодом реально пройден вживую 2026-08-28 - это больше не блокер
    // (более ранняя заметка про connection error на отправке номера устарела).
    private static final By PHONE_ENTRY_SCREEN = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Telefon raqamini kiriting\")");

    // Авторизованный профиль (проверено live 2026-08-27 на реальном аккаунте владельца
    // после успешного логина). Имя "Тимур" зашито намеренно - это тот же реальный аккаунт,
    // чей номер телефона (909023162) уже зашит в TestConfig/CheckoutFlowTest.
    private static final By AUTH_NAME = AppiumBy.accessibilityId("Тимур");
    private static final By ORDERS_CARD =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Buyurtmalar\")");
    private static final By DETAILS_CARD = AppiumBy.accessibilityId("Mening tafsilotlarim");
    private static final By ADDRESSES_CARD = AppiumBy.accessibilityId("Manzillar");
    private static final By CARDS_CARD = AppiumBy.accessibilityId("Mening kartalarim");
    private static final By LOGOUT_CARD = AppiumBy.accessibilityId("Hisobdan chiqish");

    // Диалог подтверждения выхода - заголовок/сообщение/кнопки не имеют отдельных
    // кликабельных accessibility-узлов (clickable="false" на всех content-desc), поэтому
    // "Bekor qilish" тапается по координатам центра его bounds - тот же паттерн, что и
    // BOTTOM_CTA в BaseScreen.
    private static final By LOGOUT_CONFIRM_TITLE = AppiumBy.accessibilityId("Hisobdan chiqish");
    private static final By LOGOUT_CONFIRM_MESSAGE =
            AppiumBy.accessibilityId("Haqiqatan ham hisobingizdan chiqmoqchimisiz?");
    private static final int LOGOUT_CANCEL_REF_X = 283;
    private static final int LOGOUT_CANCEL_REF_Y = 2237;

    // Кнопка подтверждения выхода в этом же диалоге - в отличие от "Bekor qilish" выше,
    // имеет СОБСТВЕННЫЙ accessibility id "Tizimdan chiqish", отличный и от заголовка
    // диалога (тот же текст "Hisobdan chiqish", что и у карточки, которая его
    // открывает), и от "Bekor qilish" - НЕ смерженный на весь экран узел (bounds
    // [639,2214][955,2261] на эталоне 1080x2400), проверено вживую 2026-08-27 через
    // page source перед тем, как полагаться на тап (см.
    // project-accidental-order-placement-incident и
    // project-accidental-address-duplication-incident в памяти проекта про риск слепых
    // координатных тапов рядом со смерженными CTA в этом приложении).
    private static final By LOGOUT_CONFIRM_BUTTON = AppiumBy.accessibilityId("Tizimdan chiqish");

    // После реального разлогинивания приложение выбрасывает не на гостевой Home и не на
    // модальный экран логина из LoginScreen (тот открывается через "Kirish" и не содержит
    // этих элементов), а на отдельный корневой экран "Telefon raqamini kiriting" с выбором
    // языка и кнопкой пропуска в углу - проверено вживую 2026-08-27 через appium-mcp.
    // "O'tkazib yuborish" - настоящая отдельно ограниченная кликабельная нода (bounds
    // [676,105][1038,168], НЕ смерженный CTA), пропускает приглашение войти и возвращает
    // на Home в гостевом состоянии.
    private static final By SKIP_LOGIN_PROMPT_BUTTON = AppiumBy.accessibilityId("O'tkazib yuborish");

    public ProfileScreen(AndroidDriver driver) {
        super(driver);
    }

    // Есть ли на экране карточка "Hisobdan chiqish" - надёжный сигнал авторизованного
    // состояния (в гостевом профиле её нет, см. verifyGuestStateShown). Используется,
    // чтобы logout() ниже можно было вызывать идемпотентно - не падать, если аккаунт уже
    // гостевой.
    public boolean isAuthorized() {
        return !driver.findElements(LOGOUT_CARD).isEmpty();
    }

    // Реально разлогинивает реальный аккаунт (см. ProfileGuestTest, где используется как
    // setup перед гостевыми тестами - см. project-real-account-live-backend в памяти
    // проекта про то, что логин на бэкенде может снова заработать между сессиями, и
    // тогда без явного logout() ProfileGuestTest падает). Доводит приложение до
    // стабильного гостевого Home, а не оставляет его посередине флоу логина - дотапывает
    // "O'tkazib yuborish" сама.
    public void logout() {
        waitFor(LOGOUT_CARD).click();
        waitFor(LOGOUT_CONFIRM_BUTTON).click();
        waitFor(SKIP_LOGIN_PROMPT_BUTTON).click();
    }

    // PROF-A01: имя, счётчик заказов и счётчик избранного отображаются в авторизованном
    // профиле.
    public ProfileScreen verifyAuthorizedStateShown() {
        Assert.assertTrue(waitFor(AUTH_NAME).isDisplayed(), "Имя пользователя не отображается в профиле");
        Assert.assertTrue(driver.findElement(ORDERS_CARD).isDisplayed(), "Карточка \"Buyurtmalar\" не отображается");
        Assert.assertTrue(driver.findElement(GUEST_FAVORITES_CARD).isDisplayed(), "Карточка \"Sevimlilar\" не отображается");
        return this;
    }

    // PROF-A02: список избранного открывается и показывает товары (shimmer уже сходит
    // к моменту проверки на быстром соединении - здесь просто ждём сам список).
    public ProfileScreen verifyFavoritesListShown() {
        waitFor(GUEST_FAVORITES_CARD).click();
        By favoritesList = AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.ScrollView\")");
        Assert.assertTrue(waitFor(favoritesList).isDisplayed(), "Список \"Sevimlilar\" не отобразился");
        driver.navigate().back();
        Assert.assertTrue(waitFor(AUTH_NAME).isDisplayed(), "Профиль не восстановился после \"Sevimlilar\"");
        return this;
    }

    // PROF-A04: отмена в диалоге выхода не разлогинивает - профиль остаётся авторизован.
    public ProfileScreen verifyLogoutCancelKeepsSession() {
        waitFor(LOGOUT_CARD).click();
        Assert.assertTrue(waitFor(LOGOUT_CONFIRM_TITLE).isDisplayed(), "Диалог подтверждения выхода не открылся");
        Assert.assertTrue(driver.findElement(LOGOUT_CONFIRM_MESSAGE).isDisplayed(),
                "Текст подтверждения выхода не отображается");
        tapAt(scaledX(LOGOUT_CANCEL_REF_X), scaledY(LOGOUT_CANCEL_REF_Y));
        Assert.assertTrue(waitFor(AUTH_NAME).isDisplayed(),
                "После отмены выхода профиль показывает гостевое/иное состояние - похоже, отмена разлогинила");
        return this;
    }

    public DetailsScreen openDetails() {
        waitFor(DETAILS_CARD).click();
        return new DetailsScreen(driver);
    }

    // ORDH-01: экран "Buyurtmalar" - проверено вживую 2026-08-28.
    public OrdersScreen openOrders() {
        waitFor(ORDERS_CARD).click();
        return new OrdersScreen(driver);
    }

    public AddressesScreen openAddresses() {
        waitFor(ADDRESSES_CARD).click();
        return new AddressesScreen(driver);
    }

    // CARD-01: Known issue 2 в exploration-notes.md ("Xatolik ro'y berdi" без retry)
    // опровергнуто владельцем 2026-08-26 - на актуальном билде грузится нормально,
    // проверено вживую 2026-08-28.
    public CardsScreen openCards() {
        waitFor(CARDS_CARD).click();
        return new CardsScreen(driver);
    }

    // SET-01..05: "Sozlamalar" доступна и гостю, и авторизованному (см. карту экранов
    // в docs/exploration-notes.md - раздел Profile перечисляет Sozlamalar в обоих
    // состояниях).
    public SettingsScreen openSettings() {
        waitFor(SETTINGS_CARD).click();
        return new SettingsScreen(driver);
    }

    // PROF-G01: гостевое состояние показывает "Kirish" и доступные без логина разделы.
    public ProfileScreen verifyGuestStateShown() {
        Assert.assertTrue(waitFor(GUEST_KIRISH_BUTTON).isDisplayed(),
                "Кнопка \"Kirish\" не отображается в гостевом профиле");
        Assert.assertTrue(driver.findElement(GUEST_FAVORITES_CARD).isDisplayed(),
                "\"Sevimlilar\" не отображается в гостевом профиле");
        Assert.assertTrue(driver.findElement(HELP_CARD).isDisplayed(),
                "\"Yordam\" не отображается в гостевом профиле");
        Assert.assertTrue(driver.findElement(SETTINGS_CARD).isDisplayed(),
                "\"Sozlamalar\" не отображается в гостевом профиле");
        return this;
    }

    // PROF-G02: "Kirish" из профиля ведёт в тот же флоу логина (экран ввода телефона),
    // что и промпт при оформлении заказа. Возвращаемся назад, не вводя номер.
    public void verifyKirishOpensLoginFlow() {
        waitFor(GUEST_KIRISH_BUTTON).click();
        Assert.assertTrue(waitFor(PHONE_ENTRY_SCREEN).isDisplayed(),
                "Экран ввода телефона не открылся по \"Kirish\" из профиля");
        driver.navigate().back();
    }
}
