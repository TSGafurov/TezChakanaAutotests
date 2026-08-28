package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * "Manzillar": ADDR-01, ADDR-03 (см. docs/exploration-notes.md - ADDR-03 уточнён:
 * тап по адресу открывает экран редактирования, а не делает адрес активным).
 *
 * Рассчитан на авторизованный старт (см. ProfileAuthorizedTest).
 */
public class AddressesTest extends BaseTest {

    @Test
    public void addressListIsShown() {
        new HomeScreen(driver)
                .openProfileTab()
                .openAddresses()
                .verifyAddressListShown();
    }

    @Test
    public void tappingAddressOpensEditScreen() {
        new HomeScreen(driver)
                .openProfileTab()
                .openAddresses()
                .verifyTappingAddressOpensEditScreen();
    }
}
