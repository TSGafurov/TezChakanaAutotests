package com.tezchakana.tests;

import com.tezchakana.screens.HomeScreen;
import com.tezchakana.screens.ProfileScreen;
import com.tezchakana.screens.SettingsScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * SET-01 (см. docs/exploration-notes.md): переключение языка интерфейса на "Русский"
 * закрепляется - сразу отражается на Home и остаётся видимым при повторном заходе в
 * Sozlamalar/Настройки. Issue 10 в exploration-notes.md документировала это как
 * нестабильное (2026-08-27, откат языка при повторном заходе) - не воспроизвелось 2/2
 * раза при живой проверке 2026-08-29, см. комментарий у SettingsScreen.selectLanguage().
 * В finally язык ВСЕГДА возвращается на узбекский независимо от результата теста - весь
 * остальной набор тестов проекта завязан на узбекские локаторы (Hammasi/Bazar/Savat/...).
 */
public class LanguageSwitchTest extends BaseTest {

    // Переведённая версия чипа "Hammasi" (верх Home) - используется только как
    // language-agnostic сигнал того, что Home реально переключился на русский сразу
    // после подтверждения выбора языка, без похода в Sozlamalar за подтверждением.
    // 2026-08-31, воспроизведено вживую: раньше здесь была нижняя вкладка "Savat"
    // (accessibilityId("Корзина")) - надёжный сигнал ТОЛЬКО пока корзина пуста. Как
    // только в корзине реального аккаунта остаётся хотя бы один товар (NoReset(true)
    // сохраняет корзину между прогонами - в этой же сессии её туда положил
    // ProductCardQuantityTest), вкладка "Savat" перестаёт показывать текст вообще и
    // заменяется зелёной плашкой суммы ("<сумма> so'm\n<N> mahsulot") - локатор
    // переставал находиться НЕ из-за проблемы с языком, а из-за состояния корзины,
    // тест падал по таймауту на этой строке 3/3 раза подряд. Чип "Hammasi"/"Все"
    // виден независимо от содержимого корзины - надёжнее.
    private static final By HAMMASI_CHIP_RUSSIAN = AppiumBy.accessibilityId("Все");

    @Test
    public void switchingToRussianPersistsAcrossRevisitAndRevertsCleanly() {
        ProfileScreen profile = new HomeScreen(driver).openProfileTab();
        SettingsScreen settings = profile.openSettings();

        try {
            HomeScreen homeAfterSwitch = settings.selectLanguage(SettingsScreen.LANGUAGE_OPTION_RUSSIAN);
            boolean homeInRussian = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> !d.findElements(HAMMASI_CHIP_RUSSIAN).isEmpty());
            Assert.assertTrue(homeInRussian, "Home не отразил русский язык сразу после подтверждения выбора");

            // Повторный заход в Sozlamalar - именно здесь issue 10 раньше фиксировала откат.
            SettingsScreen settingsRevisited = homeAfterSwitch.openProfileTab().openSettingsRegardlessOfLanguage();
            settingsRevisited.verifyCurrentLanguageValueShown("Русский");
        } finally {
            revertLanguageToUzbekSafely();
        }
    }

    // 2026-08-31, воспроизведено вживую дважды подряд: возврат на узбекский одним прямым
    // вызовом selectLanguage() на уже имеющемся объекте SettingsScreen дважды подряд
    // "залипал" - waitFor не находил ни шторку выбора языка, ни даже сам VERSION_TEXT
    // экрана Sozlamalar/Настройки в течение полных 15с, хотя непосредственно перед этим
    // тот же экран был подтверждён вживую. Точную причину (транзитная деградация
    // бэкенда в моменте - см. Known issue 1/4/9 в exploration-notes.md, вживую в той же
    // сессии наблюдалось и ложное "Пока мы не доставляем по этому адресу" для адреса с
    // покрытием - или неучтённая гонка анимации) установить не удалось, оба раза
    // РЕАЛЬНЫЙ аккаунт оставался залипшим на русском интерфейсе до ручного вмешательства.
    // Это тот случай, когда цена ошибки (весь остальной сьют перестаёт находить
    // узбекские локаторы) выше цены лишней попытки - вместо одного прямого вызова
    // заново с нуля выводим экран через HomeScreen -> openProfileTab() ->
    // openSettingsRegardlessOfLanguage(), не полагаясь на то, в каком именно состоянии
    // остался старый SettingsScreen/HomeScreen из try-блока, и повторяем всю цепочку до
    // 3 раз.
    private void revertLanguageToUzbekSafely() {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                SettingsScreen settingsScreen = new HomeScreen(driver).openProfileTab().openSettingsRegardlessOfLanguage();
                HomeScreen home = settingsScreen.selectLanguage(SettingsScreen.LANGUAGE_OPTION_UZBEK_LATIN);
                home.verifyHomeLoaded();
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        throw new IllegalStateException(
                "Не удалось вернуть язык на узбекский после нескольких попыток - "
                        + "РЕАЛЬНЫЙ аккаунт мог остаться на русском интерфейсе, требуется ручная проверка",
                lastFailure);
    }
}
