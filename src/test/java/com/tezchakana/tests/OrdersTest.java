package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * "Buyurtmalar": ORDH-01, ORDH-03 (см. docs/exploration-notes.md).
 *
 * Рассчитан на авторизованный старт (см. ProfileAuthorizedTest).
 */
public class OrdersTest extends BaseTest {

    @Test
    public void ordersListIsShown() {
        new HomeScreen(driver)
                .openProfileTab()
                .openOrders()
                .verifyOrdersShown();
    }

    @Test
    public void cancelIconOpensDialogWithoutCancellingOrder() {
        new HomeScreen(driver)
                .openProfileTab()
                .openOrders()
                .openFirstOrder()
                .verifyCancelDialogOpensAndDismiss();
    }
}
