package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * Sozlamalar (настройки): SET-01 (частично), SET-03, SET-04, SET-05
 * (см. docs/exploration-notes.md). SET-02 ("тумблер уведомлений") в текущей сборке
 * отсутствует - экран содержит только Til/FAQ/Privacy policy/версию, проверено live
 * 2026-08-27.
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
}
