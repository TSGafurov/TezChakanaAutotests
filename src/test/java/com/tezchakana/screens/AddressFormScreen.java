package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * Второй экран флоу "Manzil qo'shish" - форма с деталями адреса (Manzil belgisi,
 * Manzilning nomi, Xonadon/Podyezd/Qavat) и кнопкой "Saqlash". Поле "Manzil" уже
 * предзаполнено выбранной на предыдущем экране точкой - остальные поля можно оставить
 * пустыми, сохранение проходит и без них (проверено вживую 2026-08-28, тот же механизм,
 * что привёл к инциденту с дублированием адресов - см.
 * project-accidental-address-duplication-incident в памяти проекта).
 */
public class AddressFormScreen extends BaseScreen {

    private static final By SAQLASH = AppiumBy.accessibilityId("Saqlash");

    public AddressFormScreen(AndroidDriver driver) {
        super(driver);
    }

    // Сохраняет адрес и делает его активным для доставки - оба действия одним тапом,
    // отдельного шага "выбрать из списка" не требуется, если сюда попали через
    // HomeScreen.openAddAddressScreen() (проверено вживую 2026-08-28: после "Saqlash"
    // приложение возвращается прямо на Home с новым адресом в шапке). "Saqlash" -
    // смерженный на весь экран CTA (см. BaseScreen.tapBottomCta), поэтому тап по
    // координате, а не по элементу.
    public HomeScreen save() {
        waitFor(SAQLASH);
        tapBottomCta();
        return new HomeScreen(driver);
    }
}
