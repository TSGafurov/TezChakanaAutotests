package com.tezchakana.tests;

import com.tezchakana.config.TestConfig;
import com.tezchakana.screens.HomeScreen;
import org.testng.annotations.Test;

/**
 * HOME-05 (доп.): заведения на Home показываются только для адреса с реальным
 * покрытием доставки. В отличие от HomeAndOnboardingTest.changingAddressAffectsStoreList
 * (переключение между уже сохранёнными адресами через selectSavedAddress() - хрупко,
 * т.к. зависит от точного совпадения текста с уже сохранённой карточкой, см.
 * address.alternate=Yunusobod в config.properties, который не совпадает ни с одним
 * реально сохранённым адресом на 2026-08-28), этот класс ставит оба адреса заново через
 * поиск на карте "Manzil qo'shish" (HomeScreen.openAddAddressScreen() -> AddAddressScreen
 * -> AddressFormScreen) - не зависит от текущего состояния "Manzillar".
 *
 * ⚠️ Не идемпотентен: каждый прогон добавляет в адресную книгу ещё один адрес "Chust"
 * (владелец аккаунта осведомлён и одобрил, 2026-08-28 - см. project-real-account-live-backend
 * в памяти проекта). Адрес "Tashkent" добавляется и удаляется в рамках одного прогона, в
 * адресной книге не остаётся. Финальное состояние безопасно в любом случае: после
 * удаления Tashkent приложение само возвращает активным ранее добавленный Chust
 * (проверено вживую 2026-08-28), поэтому явного восстановления адреса в конце не
 * требуется.
 */
public class AddressCoverageTest extends BaseTest {

    @Test
    public void storesShowOnlyForAddressesWithDeliveryCoverage() {
        // Chust - известный рабочий адрес, есть покрытие доставки.
        new HomeScreen(driver)
                .openAddAddressScreen()
                .searchAndSelectFirstResult(TestConfig.chustSearchQuery())
                .confirmLocation()
                .save()
                .verifyStoresShown();

        // Tashkent - вне зоны доставки: список заведений пуст, показано предупреждение.
        new HomeScreen(driver)
                .openAddAddressScreen()
                .searchAndSelectFirstResult(TestConfig.noCoverageSearchQuery())
                .confirmLocation()
                .save()
                .verifyNoStoresShown();

        // Убираем Tashkent из адресной книги - он самый первый в списке "Manzillar" как
        // только что добавленный (см. AddressesScreen.openFirstAddressForEditing()).
        new HomeScreen(driver)
                .openProfileTab()
                .openAddresses()
                .openFirstAddressForEditing()
                .delete();
    }
}
