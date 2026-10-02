package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.PaymentScreen;
import com.tezchakana.screens.SettingsScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Регрессионные проверки багов, найденных ручным тестированием v1.1.8 (лист «Баги» в
 * docs/manual/TezChakana_TestPlan.xlsx). Пока баг не исправлен, тест ПАДАЕТ - это
 * ожидаемо: зелёный тест значит, что баг починили. Поэтому группа "known-bugs" не
 * входит в testng.xml / testng-safe.xml, а гоняется отдельно:
 *   mvn test -DsuiteXmlFile=src/test/resources/testng-known-bugs.xml
 *
 * Все проверки только читают данные или уходят с экрана без сохранения.
 */
public class KnownBugsTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(KnownBugsTest.class);

    // ADDR-02 / BUG-003: список сохранённых адресов разный в разных местах приложения.
    @Test(groups = "known-bugs")
    public void savedAddressesAreTheSameInProfileAndHomeHeader() {
        Set<String> fromHeader = new HomeScreen(driver).savedAddressTextsFromHeaderPicker();
        Set<String> fromProfile = new HomeScreen(driver)
                .openProfileTab()
                .openAddresses()
                .savedAddressTexts();
        Assert.assertEquals(fromProfile, fromHeader,
                "Адреса в Profile → Manzillar не совпадают с адресами в шапке Home");
    }

    // ADDR-07 / BUG-004: список меток адреса пустой.
    @Test(groups = "known-bugs")
    public void addressLabelListIsNotEmpty() {
        boolean empty = new HomeScreen(driver)
                .openProfileTab()
                .openAddresses()
                .openFirstAddressForEditing()
                .isAddressLabelListEmpty();
        Assert.assertFalse(empty, "Список меток адреса («Manzil belgisi») пуст: «So'rov bo'yicha hech qanday natija topilmadi»");
    }

    // DET-03 / BUG-006: корректный e-mail не активирует «Saqlash». Кнопку не нажимаем.
    @Test(groups = "known-bugs")
    public void validEmailEnablesSaveButton() {
        var details = new HomeScreen(driver).openProfileTab().openDetails();
        boolean activeBefore = details.isSaveButtonActive();
        details.enterEmail("qa.check@test.uz");
        boolean activeAfter = details.isSaveButtonActive();
        details.discardChanges();
        Assert.assertFalse(activeBefore, "«Saqlash» активна ещё до изменений - проверка цвета кнопки не работает");
        Assert.assertTrue(activeAfter, "После ввода корректного e-mail кнопка «Saqlash» осталась неактивной");
    }

    // SET-05 / BUG-016: ответ про поддержку скопирован из ответа про сертификат.
    @Test(groups = "known-bugs")
    public void faqSupportAnswerIsNotCopiedFromCertificateAnswer() {
        SettingsScreen faq = new HomeScreen(driver).openProfileTab().openSettings().openFaq();
        String supportAnswer = faq.faqAnswer("xizmatiga qanday murojaat");
        String certificateAnswer = faq.faqAnswer("Sertifikatni qanday");
        // Уходим с экрана FAQ: returnToHomeScreen() следующего теста его не узнаёт
        // (Sozlamalar - узнаёт), иначе остальные тесты класса падали по цепочке.
        driver.navigate().back();
        Assert.assertFalse(supportAnswer.isEmpty(), "Ответ на вопрос о поддержке не найден");
        Assert.assertNotEquals(supportAnswer, certificateAnswer,
                "Ответ «Qo‘llab-quvvatlash xizmatiga qanday murojaat qilaman?» совпадает с ответом про сертификат");
    }

    // CHK-06 / BUG-002: телефон получателя можно сохранить пустым. Номер всегда
    // возвращается, корзина очищается.
    @Test(groups = "known-bugs")
    public void emptyRecipientPhoneIsRejected() {
        PaymentScreen checkout = new HomeScreen(driver).openCheckoutWithGroceryItem();
        boolean accepted;
        try {
            accepted = checkout.saveRecipientPhone("");
        } finally {
            if (!checkout.recipientRowText().replaceAll("[^0-9]", "").endsWith(TestConfig.phoneNumber())) {
                checkout.saveRecipientPhone(TestConfig.phoneNumber());
            }
            checkout.leaveAndClearCart();
        }
        Assert.assertFalse(accepted, "Пустой телефон получателя сохранился без ошибки");
    }

    // CHK-06 / BUG-002: неполный номер (8 цифр вместо 9).
    @Test(groups = "known-bugs")
    public void partialRecipientPhoneIsRejected() {
        PaymentScreen checkout = new HomeScreen(driver).openCheckoutWithGroceryItem();
        boolean accepted;
        try {
            accepted = checkout.saveRecipientPhone(TestConfig.phoneNumber().substring(0, 8));
        } finally {
            if (!checkout.recipientRowText().replaceAll("[^0-9]", "").endsWith(TestConfig.phoneNumber())) {
                checkout.saveRecipientPhone(TestConfig.phoneNumber());
            }
            checkout.leaveAndClearCart();
        }
        Assert.assertFalse(accepted, "Неполный телефон получателя (8 цифр) сохранился без ошибки");
    }

    // CHK-08 / BUG-007: закрытие формы комментария крестиком всё равно применяет текст.
    @Test(groups = "known-bugs")
    public void closingCommentWithoutSavingDiscardsText() {
        PaymentScreen checkout = new HomeScreen(driver).openCheckoutWithGroceryItem();
        String commentRow;
        try {
            checkout.commentSaveStateWithoutAndWithApartment("autotestdraft", "1");
            checkout.closeCommentWithoutSaving();
            commentRow = checkout.commentRowText();
        } finally {
            if (!checkout.clearCourierComment()) {
                LOG.warn("Не удалось очистить комментарий курьеру - проверьте вручную");
            }
            checkout.leaveAndClearCart();
        }
        Assert.assertFalse(commentRow.contains("autotestdraft"),
                "Комментарий применился без «Saqlash»: " + commentRow);
    }
}
