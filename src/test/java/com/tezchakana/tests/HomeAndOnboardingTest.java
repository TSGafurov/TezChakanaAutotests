package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * Home и стартовые диалоги без похода в магазин/корзину: ONB-04, HOME-01/02/03/05/06/07 (см.
 * docs/exploration-notes.md).
 *
 * ONB-01 (первый запуск на чистых данных) и полноценный ONB-03 (диалог разрешения на
 * уведомления на каждом запуске) сознательно не реализованы: оба потребовали бы очистки
 * данных приложения (pm clear) или отзыва уже выданного разрешения, а это противоречит
 * принятому в проекте правилу не форсировать релогин/сброс состояния реального аккаунта
 * (см. feedback-login-otp в памяти проекта). ONB-04 не требует такой очистки - диалог
 * подтверждения адреса воспроизводится при обычном перезапуске процесса приложения.
 *
 * Раньше эти тесты и STORE/CART-тесты жили в одном классе HomeAndCatalogTest (10 @Test) -
 * разделены 2026-08-25 после того, как выяснилось, что полный прогон всех 10 подряд в одной
 * JVM даёт ~70% ложных падений по таймауту (деградация эмулятора/UiAutomator2 от большого
 * числа последовательных Appium-сессий в одном прогоне), хотя каждый тест по отдельности
 * или малой группой проходит чисто - см. [[project-session-churn-flakiness]] в памяти
 * проекта. Меньшие по размеру классы тестов снижают риск такой деградации.
 */
public class HomeAndOnboardingTest extends BaseTest {

    @Test
    public void homeScreenLoadsWithChipsAndStoreList() {
        new HomeScreen(driver).verifyHomeLoaded();
    }

    @Test
    public void hammasiChipRendersStoreListAfterAnotherChip() {
        new HomeScreen(driver).verifyHammasiRendersListAfterAnotherChip();
    }

    @Test
    public void tappingChipFiltersStoreList() {
        new HomeScreen(driver).verifyChipFiltersStoreList();
    }

    @Test
    public void favoritesIconOpensFavoritesScreen() {
        new HomeScreen(driver).verifyFavoritesIconOpensFavoritesScreen();
    }

    @Test
    public void storeListScrollRevealsMoreStores() {
        new HomeScreen(driver).verifyStoreListScrollRevealsMoreStores();
    }

    @Test
    public void changingAddressAffectsStoreList() {
        HomeScreen homeScreen = new HomeScreen(driver);

        homeScreen.openAddressPickerFromHeader()
                .selectSavedAddress(TestConfig.alternateAddressLabel())
                .verifyNoDeliveryCoverageMessageShown();

        // Восстанавливаем исходный адрес - иначе реальный аккаунт остался бы с
        // изменённым адресом доставки после теста.
        homeScreen.openAddressPickerFromHeader().selectSavedAddress(TestConfig.defaultAddressLabel());
    }

    // ONB-04: перезапуск процесса приложения (не сессии Appium) заново показывает
    // диалог подтверждения адреса - воспроизводится не только на первом запуске.
    @Test
    public void restartingAppShowsAddressConfirmationDialogWithChangeOption() {
        driver.terminateApp(TestConfig.appPackage());
        driver.activateApp(TestConfig.appPackage());

        new HomeScreen(driver)
                .verifyStartupAddressDialogShown()
                .verifyChangeAddressOpensPickerFromStartupDialog()
                .verifyHomeLoaded();
    }
}
