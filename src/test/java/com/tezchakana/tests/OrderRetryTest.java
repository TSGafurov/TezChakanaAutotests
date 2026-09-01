package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.CartScreen;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.LoginScreen;
import com.tezchakana.screens.OtpScreen;
import com.tezchakana.screens.PaymentScreen;
import com.tezchakana.screens.StoreScreen;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * ORD-04 (см. docs/exploration-notes.md): тап "Qayta urinib ko'ring" на экране ошибки
 * оплаты не ломает приложение и оставляет его в том же понятном error-state, а не
 * зависает/крашится/уводит в неожиданное место. Сеть намеренно НЕ восстанавливается
 * между первой попыткой и ретраем - технически надёжного способа доказать в этом
 * приложении, что тап именно ЗАПУСТИЛ новый сетевой запрос (а не просто пришёлся
 * мимо), нет: экран ошибки - Flutter-виджет, чьё element/semantics-дерево не меняет
 * identity при setState-ребилде, поэтому ни "исчезновение узла", ни staleness
 * WebElement ничего не доказывают (проверено вживую 2026-08-29 обоими способами, см.
 * комментарий у PaymentScreen.retryOrderFromError()). Не пытаемся довести реальный
 * платёж до успеха - см. ORD-01 (заблокирован нестабильностью бэкенда) про то, почему
 * реальное успешное оформление заказа сознательно не автоматизируется в этом тесте.
 *
 * 2026-08-31: этот тест (наравне с OrderErrorTest) вносил вклад в пул реальных
 * заказов, случайно созданных из-за гонки в старом toggleNetwork() - см.
 * BaseTest.toggleNetwork() и ORD-02/ORD-04 в exploration-notes.md. Теперь toggleNetwork()
 * дожидается физического подтверждения отключения сети, а этот тест дополнительно
 * сверяет самый свежий заказ реального аккаунта до и после.
 */
public class OrderRetryTest extends BaseTest {

    @Test
    public void retryButtonReattemptsPlaceOrder() throws InterruptedException, IOException {
        String topOrderBefore = new HomeScreen(driver).openProfileTab().openOrders().topOrderNumber();
        // См. идентичный комментарий в OrderErrorTest - OrdersScreen без нижней
        // навигации, returnToHomeScreen() не восстановится отсюда без явного back().
        driver.navigate().back();

        StoreScreen storeScreen = new HomeScreen(driver).openBazarTab()
                .openStore(TestConfig.storeName())
                .scrollToCategory(TestConfig.groceryCategoryLabel())
                .addProductToCart(TestConfig.groceryProductName());

        CartScreen cartScreen = storeScreen.openCartSummaryBar();
        LoginScreen loginScreen = cartScreen.proceedToCheckout();

        PaymentScreen paymentScreen;
        if (loginScreen.isLoginPromptShown()) {
            OtpScreen otpScreen = loginScreen
                    .confirmPrompt()
                    .submitPhoneNumber(TestConfig.phoneNumber());
            otpScreen.enterCode(TestConfig.otpCode());
            paymentScreen = otpScreen.confirmCode()
                    .handleNotificationsPermission()
                    .handleLocationPermission();
        } else {
            paymentScreen = new PaymentScreen(driver);
        }

        paymentScreen.selectCashPayment();

        try {
            toggleNetwork(false);
            paymentScreen.placeOrder();
            paymentScreen.verifyOrderErrorShown();

            // ORD-04: сеть всё ещё отключена (и уже физически подтверждена
            // toggleNetwork() выше) - ретрай должен снова упереться в ту же ошибку, а
            // не зависнуть/провалиться молча.
            paymentScreen.retryOrderFromError();
            paymentScreen.verifyOrderErrorShown();
        } finally {
            toggleNetwork(true);
        }

        HomeScreen home = paymentScreen.goHomeFromOrderError().verifyHomeLoaded();

        Thread.sleep(3000);
        String topOrderAfter = home.openProfileTab().openOrders().topOrderNumber();
        driver.navigate().back();
        Assert.assertEquals(topOrderAfter, topOrderBefore,
                "Появился новый заказ (\"" + topOrderAfter + "\") несмотря на error-state оплаты - "
                        + "реальный платёж, похоже, всё-таки прошёл");
    }
}
