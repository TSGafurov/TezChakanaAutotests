package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * "Mening tafsilotlarim": DET-01 (см. docs/exploration-notes.md). DET-02 не
 * автоматизирован - см. комментарий в DetailsScreen. DET-03 ("Hisobni o'chirish") не
 * автоматизирован намеренно: необратимое удаление реального аккаунта.
 *
 * Рассчитан на авторизованный старт (см. ProfileAuthorizedTest).
 */
public class DetailsTest extends BaseTest {

    @Test(groups = "safe")
    public void detailsScreenShowsNamePhoneAndSocialButtons() {
        new HomeScreen(driver)
                .openProfileTab()
                .openDetails()
                .verifyDetailsShown();
    }
}
