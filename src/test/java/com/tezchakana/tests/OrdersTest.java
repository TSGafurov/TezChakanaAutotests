package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.OrdersScreen;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * "Buyurtmalar": ORDH-01, ORDH-02, ORDH-03 (см. docs/exploration-notes.md).
 *
 * Рассчитан на авторизованный старт (см. ProfileAuthorizedTest).
 */
public class OrdersTest extends BaseTest {

    @Test(groups = "safe")
    public void ordersListIsShown() {
        new HomeScreen(driver)
                .openProfileTab()
                .openOrders()
                .verifyOrdersShown();
    }

    @Test(groups = "safe")
    public void cancelIconOpensDialogWithoutCancellingOrder() {
        // Отмена возможна только у активного заказа на ранней стадии. Реальные заказы
        // тест не создаёт - если активных нет, честно пропускается.
        OrdersScreen orders = new HomeScreen(driver).openProfileTab().openOrders();
        if (!orders.hasActiveOrders()) {
            throw new SkipException("Нет активных заказов - проверять отмену не на чем");
        }
        orders.openFirstOrder().verifyCancelDialogOpensAndDismiss();
    }

    @Test(groups = "safe")
    public void orderDetailsShowConsistentOrderInfo() {
        new HomeScreen(driver)
                .openProfileTab()
                .openOrders()
                .showAnyOrders()
                .openFirstOrder()
                .verifyOrderDetailsShowConsistentInfo();
    }
}
