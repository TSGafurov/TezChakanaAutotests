package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * Sozlamalar (настройки): SET-01 (частично), SET-02, SET-03, SET-04, SET-05
 * (см. docs/exploration-notes.md). SET-02 ("тумблер уведомлений") отсутствовал в билде
 * 2026-08-27 (проверено live) - появился в билде на 2026-08-31, покрыт отдельным тестом.
 *
 * Sozlamalar доступна и гостю, и авторизованному, поэтому старт-состояние сессии
 * (в отличие от CheckoutFlowTest/ProfileGuestTest) не важно для этого класса.
 */
public class SettingsTest extends BaseTest {

    @Test
    public void settingsScreenShowsAllRows() {
        new HomeScreen(driver)
                .openProfileTab()
                .openSettings()
                .verifyScreenShown();
    }

    @Test
    public void languagePickerShowsCurrentAndOptions() {
        new HomeScreen(driver)
                .openProfileTab()
                .openSettings()
                .verifyLanguagePickerShowsOptions();
    }

    @Test
    public void faqOpensInApp() {
        new HomeScreen(driver)
                .openProfileTab()
                .openSettings()
                .verifyFaqOpensInApp();
    }

    @Test
    public void privacyPolicyOpensInApp() {
        new HomeScreen(driver)
                .openProfileTab()
                .openSettings()
                .verifyPrivacyPolicyOpensInApp();
    }

    @Test
    public void notificationsToggleEnablesDirectlyAndDisablesWithConfirmation() {
        new HomeScreen(driver)
                .openProfileTab()
                .openSettings()
                .verifyNotificationsToggleEnableAndDisableWithConfirmation();
    }
}
