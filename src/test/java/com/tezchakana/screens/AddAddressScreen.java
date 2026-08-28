package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Экран "Manzil qo'shish" (карта + поиск), первый из двух экранов флоу добавления
 * адреса - см. AddressFormScreen для второго (форма с "Saqlash"). Проверено вживую
 * 2026-08-28: этот экран открывается и с шапки Home (HomeScreen.openAddAddressScreen()),
 * и из "Manzillar" в профиле - в обоих случаях ведёт сюда же.
 */
public class AddAddressScreen extends BaseScreen {

    private static final By SEARCH_FIELD = AppiumBy.className("android.widget.EditText");

    // Первый результат из выпадающего списка поиска. У результатов нет общего
    // accessibility id/паттерна текста (гео-подсказки Google Maps, текст результата
    // часто не совпадает с введённым запросом - например, запрос "tashkent" даёт
    // первым результатом "Toshkent, 100000, O'zbekiston"), поэтому матчить по
    // содержимому введённого запроса ненадёжно.
    //
    // Две отброшенные попытки, прежде чем дошли до этой:
    // 1) EditText[@hint="Qidirish"] - атрибут hint пропадает из дерева доступности, как
    //    только в поле есть текст.
    // 2) View[@is-collection="true"] - похоже на надёжный маркер контейнера результатов
    //    при проверке через appium-mcp вживую, но при диагностике реального падения
    //    теста (2026-08-28, сохранённый на устройстве driver.getPageSource() в момент
    //    таймаута) выяснилось, что этого атрибута в page source, который получает сам
    //    Selenium/Appium Java-клиент, попросту НЕТ - appium-mcp, судя по всему,
    //    добавляет какие-то свои дополнительные атрибуты, которых нет в "сыром" дампе.
    //    Локатор, проверенный только через appium-mcp, не гарантированно работает в
    //    самом коде теста - нужно матчить по тому, что реально есть в обоих случаях.
    // Итоговый путь опирается только на структуру дерева (родитель поля поиска -> его
    // следующий View-сиблинг с результатами -> первый ImageView внутри), которая
    // одинакова в обоих дампах.
    private static final By FIRST_SEARCH_RESULT = AppiumBy.xpath(
            "//android.widget.EditText/parent::android.view.View"
                    + "/following-sibling::android.view.View[1]//android.widget.ImageView[1]");

    // Гео-подсказки Google Maps - реальный сетевой запрос, а не мгновенный локальный
    // фильтр (проверено вживую 2026-08-28 - ручной ввод стабильно укладывался в ~13с).
    // Небольшой запас сверх обычного WAIT_TIMEOUT (15с) на случай сетевых колебаний.
    private static final Duration SEARCH_RESULTS_TIMEOUT = Duration.ofSeconds(20);

    public AddAddressScreen(AndroidDriver driver) {
        super(driver);
    }

    // Вводим запрос через adb (не Selenium sendKeys/setValue) - тот же паттерн, что и
    // в OtpScreen.enterCode(): поле на этом экране один раз не приняло текст через
    // Appium setValue без видимой ошибки (проверено вживую 2026-08-28), adb shell input
    // text сработал стабильно во всех попытках. Пауза перед вводом - тот же самый
    // паттерн и по той же причине, что и в OtpScreen.enterCode() (input connection
    // готова не сразу после click()).
    public AddAddressScreen searchAndSelectFirstResult(String query) {
        waitFor(SEARCH_FIELD).click();
        sleep(Duration.ofSeconds(1));
        typeViaAdb(query);
        new WebDriverWait(driver, SEARCH_RESULTS_TIMEOUT)
                .until(d -> !d.findElements(FIRST_SEARCH_RESULT).isEmpty());
        driver.findElement(FIRST_SEARCH_RESULT).click();
        return this;
    }

    // Подтверждает выбранную на карте точку ("Tasdiqlash") и переходит к форме с
    // деталями адреса. Тот же смерженный на весь экран CTA-паттерн, что и везде в
    // приложении (см. BaseScreen.tapBottomCta) - небольшая пауза перед тапом даёт карте
    // долистать анимацию перемещения метки после выбора результата поиска.
    public AddressFormScreen confirmLocation() {
        sleep(Duration.ofMillis(500));
        tapBottomCta();
        return new AddressFormScreen(driver);
    }
}
