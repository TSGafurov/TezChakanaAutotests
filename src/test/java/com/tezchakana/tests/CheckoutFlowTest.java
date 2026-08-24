package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.CartScreen;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.OtpScreen;
import com.tezchakana.screens.StoreScreen;
import org.testng.annotations.Test;

/**
 * Checkout flow, locators taken from Appium Inspector XML page-source snapshots
 * (see /Users/timurgafurov/Desktop/TezChakanaXML). The app is a Flutter app whose
 * elements expose almost no resource-id/text - screen-specific locators and their
 * coordinate-tap workarounds live in the com.tezchakana.screens classes, this test
 * only orchestrates the flow through them.
 */
public class CheckoutFlowTest extends BaseTest {

    @Test
    public void completeCheckoutWithCashOnDelivery() {
        StoreScreen storeScreen = new HomeScreen(driver)
                .openBazarTab()
                .openStore(TestConfig.storeName());

        CartScreen cartScreen = storeScreen
                .scrollToCategory(TestConfig.waterCategoryLabel())
                .addProductToCart(TestConfig.productName())
                .openCartSummaryBar();

        OtpScreen otpScreen = cartScreen
                .proceedToCheckout()
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
    }
}
