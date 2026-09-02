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
 * ORD-02 (см. docs/exploration-notes.md, Known issue 1): error-state оплаты,
 * спровоцированный детерминированно - отключением wifi/data через adb прямо перед
 * "Buyurtma qilish", вместо ожидания естественной нестабильности бэкенда.
 *
 * 2026-08-31: раньше здесь утверждалось "реальный платёж не создаётся - запрос не
 * долетает до бэкенда" - это оказалось НЕВЕРНО. Тест дважды подряд оставлял реальные
 * подтверждённые заказы (TEZ00167/TEZ00168, отменены через приложение), несмотря на
 * то что error-state на экране корректно отображался. Причина - гонка между тапом
 * "Buyurtma qilish" и фактическим (не только логическим) отключением сети, см.
 * BaseTest.toggleNetwork(). Теперь toggleNetwork() дожидается физического подтверждения
 * через /proc/net/route, а этот тест ДОПОЛНИТЕЛЬНО сверяет самый свежий заказ реального
 * аккаунта до и после - если хоть один новый заказ появился, тест падает явно, вместо
 * того чтобы полагаться только на состояние экрана.
 */
public class OrderErrorTest extends BaseTest {

    @Test(groups = "destructive")
    public void networkFailureDuringPlaceOrderShowsErrorState() throws InterruptedException, IOException {
        String topOrderBefore = new HomeScreen(driver).openProfileTab().openOrders().topOrderNumber();
        // OrdersScreen - "пушнутый" экран без нижней навигации; returnToHomeScreen()
        // (используется дальше в openBazarTab()) умеет только слепо тапать по
        // координате вкладки Home и не восстанавливается с экрана, где такой вкладки
        // физически нет - без явного back() отсюда следующий шаг зависал по таймауту
        // в ожидании "Bazar" (обнаружено вживую 2026-08-31 сразу после добавления
        // этого снимка "до"). Один back() с "Buyurtmalar" ведёт на Profile (с нижней
        // навигацией), откуда returnToHomeScreen() уже умеет добираться до Home.
        driver.navigate().back();

        StoreScreen storeScreen = new HomeScreen(driver).openBazarTab()
                .openStore(TestConfig.storeName())
                .scrollToCategory(TestConfig.groceryCategoryLabel())
                .addProductToCart(TestConfig.groceryProductName());

        CartScreen cartScreen = storeScreen.openCartSummaryBar();
        LoginScreen loginScreen = cartScreen.proceedToCheckout();

        // CHK-01: гостевой логин со статическим тестовым OTP (см.
        // [[tezchakana-login-preference]] в памяти проекта - реальный SMS не отправляется).
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
        } finally {
            toggleNetwork(true);
        }

        // "Bosh sahifa" тапаем уже после восстановления сети (риска повторного реального
        // платежа нет - кнопка ведёт на Home, а не повторяет "Buyurtma qilish"), чтобы
        // подтвердить, что приложение не падает и штатно возвращается в рабочее
        // состояние после ошибки.
        HomeScreen home = paymentScreen.goHomeFromOrderError().verifyHomeLoaded();

        // Даём бэкенду немного времени на любую отложенную обработку прежде, чем
        // сверять список заказов - именно в этом окне реальный заказ проскочил раньше.
        Thread.sleep(3000);
        String topOrderAfter = home.openProfileTab().openOrders().topOrderNumber();
        // Возврат на Profile (см. комментарий у первого back() в начале теста) - чтобы
        // не оставлять приложение на "пушнутом" экране без нижней навигации для
        // следующего теста сьюта.
        driver.navigate().back();
        Assert.assertEquals(topOrderAfter, topOrderBefore,
                "Появился новый заказ (\"" + topOrderAfter + "\") несмотря на error-state оплаты - "
                        + "реальный платёж, похоже, всё-таки прошёл");
    }
}
