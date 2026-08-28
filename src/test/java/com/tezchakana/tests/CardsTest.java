package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * "Mening kartalarim": CARD-01 (см. docs/exploration-notes.md).
 *
 * Рассчитан на авторизованный старт (см. ProfileAuthorizedTest).
 */
public class CardsTest extends BaseTest {

    @Test
    public void cardsListIsShown() {
        new HomeScreen(driver)
                .openProfileTab()
                .openCards()
                .verifyCardsShown();
    }
}
