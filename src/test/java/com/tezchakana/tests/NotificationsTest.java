package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * "Xabarnomalar": NOTIF-01 (см. docs/exploration-notes.md).
 *
 * Рассчитан на авторизованный старт (см. ProfileAuthorizedTest). Read-only - не
 * мутирует реальный аккаунт.
 */
public class NotificationsTest extends BaseTest {

    @Test
    public void notificationsListIsShown() {
        new HomeScreen(driver)
                .openProfileTab()
                .openNotifications()
                .verifyNotificationsShown();
    }
}
