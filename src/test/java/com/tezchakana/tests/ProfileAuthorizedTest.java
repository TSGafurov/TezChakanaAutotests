package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * Профиль в авторизованном состоянии: PROF-A01, PROF-A02, PROF-A04
 * (см. docs/exploration-notes.md).
 *
 * Как и большинство тестовых классов проекта (кроме CheckoutFlowTest/ProfileGuestTest),
 * этот класс рассчитан на уже АВТОРИЗОВАННЫЙ старт - `NoReset(true)` сохраняет сессию
 * логина между тестами/классами. Аккаунт авторизован с 2026-08-27 (см.
 * project-real-account-live-backend в памяти проекта) - если этот класс начнёт падать
 * из-за гостевого состояния, значит сессия разлогинилась и её нужно восстановить
 * (см. LoginScreen/OtpScreen) перед этим классом.
 */
public class ProfileAuthorizedTest extends BaseTest {

    @Test(groups = "safe")
    public void authorizedProfileShowsNameAndCounters() {
        new HomeScreen(driver)
                .openProfileTab()
                .verifyAuthorizedStateShown();
    }

    @Test(groups = "safe")
    public void favoritesListOpensFromProfile() {
        new HomeScreen(driver)
                .openProfileTab()
                .verifyFavoritesListShown();
    }

    @Test(groups = "safe")
    public void cancellingLogoutKeepsSessionAuthorized() {
        new HomeScreen(driver)
                .openProfileTab()
                .verifyLogoutCancelKeepsSession();
    }
}
