package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.CartScreen;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.LoginScreen;
import com.tezchakana.screens.PaymentScreen;
import com.tezchakana.screens.StoreScreen;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Экран чекаута ("Xarid qilish") до кнопки оформления заказа: CART-05, CHK-03, CHK-05
 * (см. docs/exploration-notes.md). Останавливается перед "Buyurtma qilish" - реальное
 * оформление заказа покрыто отдельно в CheckoutFlowTest. CHK-04 (кнопка оформления
 * неактивна без выбора оплаты) сознательно не реализован: в дереве доступности
 * enabled/clickable у смерженной CTA остаются true независимо от состояния выбора
 * (см. PaymentScreen), поэтому единственный способ это проверить - тапнуть по кнопке
 * без выбранного способа оплаты и убедиться, что заказ не оформился, а на реальном
 * аккаунте это слишком рискованно без стопроцентной уверенности в результате.
 */
public class CheckoutDetailsTest extends BaseTest {

    @Test(groups = "mutating")
    public void checkoutScreenShowsDeliveryDetailsAndReflectsPaymentSelection() {
        StoreScreen storeScreen = new HomeScreen(driver)
                .openBazarTab()
                .openStore(TestConfig.storeName());

        CartScreen cartScreen = storeScreen
                .scrollToCategory(TestConfig.groceryCategoryLabel())
                .addProductToCart(TestConfig.groceryProductName())
                .openCartSummaryBar();

        LoginScreen loginScreen = cartScreen.proceedToCheckout();

        // CART-05: раньше пропуск экрана логина для авторизованного аккаунта был лишь
        // неявным допущением в комментарии ("берём фактический PaymentScreen напрямую,
        // полагаясь на то, что аккаунт уже авторизован") - явный ассерт вместо тихого
        // предположения, отдельно от CHK-01/CHK-02, где это ветвление проверяется только
        // косвенно (см. feedback-login-otp в памяти проекта про то, почему сам логин не
        // автоматизируется).
        Assert.assertFalse(loginScreen.isLoginPromptShown(),
                "Экран логина показан авторизованному пользователю при переходе в чекаут (CART-05)");

        CartScreen leftoverCart = new PaymentScreen(driver)
                .verifyDeliveryDetailsDisplayed()
                .selectCashPayment()
                .verifyCashPaymentSelected()
                .goBackToCart();

        // 2026-09-11: см. javadoc PaymentScreen.goBackToCart() - без этой очистки товар и
        // открытый чекаут переживали конец теста и ломали каскадом весь остаток прогона
        // testng.xml (воспроизведено вживую, тот же случай, что и в CheckoutFlowTest).
        leftoverCart.clearCart();
        leftoverCart.close();
    }
}
