package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.SettingsScreen;
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
}
