package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.CartScreen;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.LoginScreen;
import com.tezchakana.screens.OtpScreen;
import com.tezchakana.screens.PaymentScreen;
import com.tezchakana.screens.StoreScreen;
import org.testng.annotations.Test;

/**
 * Checkout flow, locators taken from Appium Inspector XML page-source snapshots
 * (see /Users/timurgafurov/Desktop/TezChakanaXML). The app is a Flutter app whose
 * elements expose almost no resource-id/text - screen-specific locators and their
 * coordinate-tap workarounds live in the com.tezchakana.screens classes, this test
 * only orchestrates the flow through them.
 *
 * CHK-01 (гостевой логин посреди чекаута) - основная цель этого класса, но
 * `NoReset(true)` может застать аккаунт уже авторизованным (см.
 * project-real-account-live-backend в памяти проекта: логин на бэкенде то ломался, то
 * снова работал в течение одного дня). 2026-08-27 обнаружено вживую: если аккаунт уже
 * авторизован, тап "To'lovga o'tish" ведёт СРАЗУ на экран чекаута (PaymentScreen), минуя
 * промпт логина, и `LoginScreen.confirmPrompt()` падал по таймауту, ожидая узел "Kirish",
 * которого не будет (без тапа вслепую - см. LoginScreen.confirmPrompt(), он ждёт узел,
 * прежде чем тапнуть). Приложение при этом оставалось на реальном экране чекаута до конца
 * прогона, что каскадом ломало следующие тестовые классы (см. HomeScreen.returnToHomeScreen(),
 * который не рассчитан на восстановление с этого экрана). Метод теперь через
 * LoginScreen.isLoginPromptShown() определяет, какой случай сейчас, и не полагается на
 * гостевой старт - тот же паттерн уже применён в CheckoutDetailsTest.
 */
public class CheckoutFlowTest extends BaseTest {

    @Test(groups = "mutating")
    public void completeCheckoutWithCashOnDelivery() {
        StoreScreen storeScreen = new HomeScreen(driver)
                .openBazarTab()
                .openStore(TestConfig.storeName());

        CartScreen cartScreen = storeScreen
                .scrollToCategory(TestConfig.waterCategoryLabel())
                .addProductToCart(TestConfig.productName())
                .openCartSummaryBar();

        LoginScreen loginScreen = cartScreen.proceedToCheckout();

        if (loginScreen.isLoginPromptShown()) {
            try {
                OtpScreen otpScreen = loginScreen
                        .confirmPrompt()
                        .submitPhoneNumber(TestConfig.phoneNumber());

                otpScreen.enterCode(TestConfig.otpCode());

                // Дальше пока не идём: подтверждение OTP ("Tasdiqlash") не вызывается, пока не
                // подтверждено, что тестовый bypass-код реально принимается бэкендом. Как только
                // будет добро - раскомментировать и продолжить цепочку:
                //
                // otpScreen.confirmCode()
                //         .handleNotificationsPermission()
                //         .handleLocationPermission()
                //         .selectCashPayment()
                //         .placeOrder()
                //         .verifyOrderSuccess();
            } finally {
                // 2026-09-16: гостевая ветка тоже оставляла товар в корзине (добавляется
                // безусловно выше, до проверки isLoginPromptShown()) - воспроизведено вживую:
                // SYS-17 ("Davom etish" на экране ввода телефона периодически не реагирует)
                // остановил тест прямо на этом экране, а товар пережил конец сессии и засорил
                // следующие тесты в сьюте (тот же класс проблемы, что и SYS-13 для
                // авторизованной ветки ниже, см. exploration-notes.md). returnToHomeScreen()
                // уже умеет уходить с экрана ввода телефона (тоже SYS-17) - переоткрываем
                // магазин обычным путём и чистим корзину, если она не пуста.
                StoreScreen recoveryStore = new HomeScreen(driver)
                        .openBazarTab()
                        .openStore(TestConfig.storeName());
                if (recoveryStore.hasItemsInCart()) {
                    recoveryStore.openCartSummaryBar().clearCart();
                }
            }
        } else {
            // Авторизованный старт - promptа не было, "To'lovga o'tish" привёл сразу на
            // экран чекаута (тот же случай, что и в CheckoutDetailsTest).
            CartScreen leftoverCart = new PaymentScreen(driver)
                    .verifyDeliveryDetailsDisplayed()
                    .goBackToCart();

            // 2026-09-11: см. javadoc PaymentScreen.goBackToCart() - без этой очистки товар
            // и открытый чекаут переживали конец теста и ломали каскадом весь остаток
            // прогона testng.xml (воспроизведено вживую).
            leftoverCart.clearCart();
            leftoverCart.close();
        }
    }
}
