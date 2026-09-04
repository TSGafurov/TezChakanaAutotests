package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.ProfileScreen;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URI;
import java.time.Duration;

/**
 * Профиль в гостевом состоянии: PROF-G01, PROF-G02 (см. docs/exploration-notes.md).
 *
 * В отличие от остальных тестовых классов проекта, этот рассчитан на РАЗЛОГИНЕННЫЙ
 * старт, а не на уже авторизованную сессию (аналогично CheckoutFlowTest) - сессия
 * `NoReset(true)` сохраняет то состояние логина, в котором её оставил предыдущий
 * тест/сессия. Логин на реальном бэкенде уже один раз "чинился" сам в течение одного
 * дня (см. project-real-account-live-backend в памяти проекта), поэтому полагаться на
 * то, что аккаунт "и так" гостевой, ненадёжно - {@link #ensureGuestState()} ниже явно
 * разлогинивает реальный аккаунт перед этим классом, если он окажется авторизован
 * (идемпотентно - ничего не делает, если он уже гостевой).
 */
public class ProfileGuestTest extends BaseTest {

    // Отдельная короткоживущая Appium-сессия только для logout(): `driver` из BaseTest
    // создаётся в @BeforeMethod (то есть только перед первым @Test), а этот шаг должен
    // отработать один раз до всех @Test методов класса, поэтому не может полагаться на
    // порядок выполнения @Test (TestNG его не гарантирует).
    @BeforeClass
    public void ensureGuestState() throws Exception {
        // udid явно закреплён - тот же риск ambiguous-device, что и в BaseTest.setUp()
        // (см. комментарий там), эта сессия создаётся отдельно и не наследует опции
        // оттуда.
        UiAutomator2Options options = new UiAutomator2Options()
                .setUdid(ADB_DEVICE)
                .setAppPackage(TestConfig.appPackage())
                .setAppActivity(TestConfig.appActivity())
                .setNoReset(true)
                .setAutoGrantPermissions(false)
                .setNewCommandTimeout(Duration.ofSeconds(120));

        AndroidDriver setupDriver = new AndroidDriver(new URI(TestConfig.appiumUrl()).toURL(), options);
        try {
            ProfileScreen profileScreen = new HomeScreen(setupDriver).openProfileTab();
            if (profileScreen.isAuthorized()) {
                profileScreen.logout();
            }
        } finally {
            setupDriver.quit();
        }
    }

    @Test(groups = "mutating")
    public void guestProfileShowsPlaceholderAndKirish() {
        new HomeScreen(driver)
                .openProfileTab()
                .verifyGuestStateShown();
    }

    @Test(groups = "mutating")
    public void kirishFromGuestProfileOpensLoginFlow() {
        new HomeScreen(driver)
                .openProfileTab()
                .verifyKirishOpensLoginFlow();
    }
}
