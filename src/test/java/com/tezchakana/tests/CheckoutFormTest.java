package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.PaymentScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Экран оформления «Xarid qilish»: подэкраны времени доставки, комментария курьеру и
 * промокода (CHK-04, CHK-07, CHK-12 в docs/manual/TezChakana_TestPlan.xlsx).
 *
 * Реальный заказ НЕ оформляется: «Buyurtma qilish» не нажимается, а нижние кнопки
 * подэкранов нажимаются только после проверки, что открыт нужный подэкран (см.
 * PaymentScreen.tapSubScreenCta()). Каждый тест кладёт в корзину один товар и в конце
 * очищает корзину.
 */
public class CheckoutFormTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(CheckoutFormTest.class);

    // CHK-04: выбор времени показывает «Yaqin 2 soat» и слоты на сегодня/завтра.
    @Test(groups = "mutating")
    public void deliveryTimeScreenShowsSoonAndScheduledSlots() {
        PaymentScreen checkout = new HomeScreen(driver).openCheckoutWithGroceryItem();
        try {
            checkout.verifyDeliveryTimeOptionsShown();
        } finally {
            checkout.leaveAndClearCart();
        }
    }

    // CHK-07: без обязательного «Xonadon» комментарий сохранить нельзя.
    @Test(groups = "mutating")
    public void courierCommentRequiresApartment() {
        PaymentScreen checkout = new HomeScreen(driver).openCheckoutWithGroceryItem();
        boolean[] saveActive;
        try {
            saveActive = checkout.commentSaveStateWithoutAndWithApartment("autotest", "1");
            checkout.closeCommentWithoutSaving();
        } finally {
            if (!checkout.clearCourierComment()) {
                LOG.warn("Не удалось очистить комментарий курьеру - проверьте вручную");
            }
            checkout.leaveAndClearCart();
        }
        Assert.assertFalse(saveActive[0], "«Saqlash» активна без обязательного поля «Xonadon»");
        Assert.assertTrue(saveActive[1], "«Saqlash» не активировалась после заполнения «Xonadon»");
    }

    // CHK-12: неверный промокод не применяется и не меняет итог.
    @Test(groups = "mutating")
    public void invalidPromoCodeIsNotApplied() {
        PaymentScreen checkout = new HomeScreen(driver).openCheckoutWithGroceryItem();
        String totalsBefore;
        String totalsAfter;
        String promoRow;
        try {
            totalsBefore = checkout.totalsText();
            checkout.submitPromoCode("AUTOTEST000");
            totalsAfter = checkout.totalsText();
            promoRow = checkout.promoRowText();
        } finally {
            checkout.leaveAndClearCart();
        }
        Assert.assertEquals(totalsAfter, totalsBefore, "Итог изменился после неверного промокода");
        Assert.assertFalse(promoRow.contains("AUTOTEST000"),
                "Неверный промокод отображается как применённый: " + promoRow);
    }
}
