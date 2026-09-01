package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

public class SettingsScreen extends BaseScreen {

    // Все локаторы проверены live на реальном аккаунте 2026-08-27 в гостевом состоянии
    // (Sozlamalar доступна без логина).
    private static final By TIL_ROW =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Til\")");
    private static final By FAQ_ROW = AppiumBy.accessibilityId("Ko‘p beriladigan savollar");
    private static final By PRIVACY_ROW = AppiumBy.accessibilityId("Ilova maxfiylik siyosati");
    private static final By VERSION_TEXT =
            AppiumBy.androidUIAutomator("new UiSelector().descriptionStartsWith(\"V \")");

    private static final By LANGUAGE_DIALOG_TITLE = AppiumBy.accessibilityId("Tilni tanlash");
    // public - буквальные названия языков, не переведённые лейблы, поэтому безопасно
    // переиспользовать как стабильные локаторы из тестов (см. selectLanguage() ниже).
    public static final By LANGUAGE_OPTION_UZBEK_LATIN = AppiumBy.accessibilityId("O‘zbekcha");
    public static final By LANGUAGE_OPTION_RUSSIAN = AppiumBy.accessibilityId("Русский");

    // Крестик закрытия шторки "Tilni tanlash" - без content-desc, тот же паттерн
    // coordinate-tap, что и BOTTOM_CTA в BaseScreen (см. bounds [933,1576][1080,1723]
    // на эталонном экране 1080x2400, центр x=1006/y=1649).
    private static final int LANGUAGE_DIALOG_CLOSE_REF_X = 1006;
    private static final int LANGUAGE_DIALOG_CLOSE_REF_Y = 1649;

    // SET-01: строка "Til"/"Язык" и кнопка подтверждения шторки "Tasdiqlash"/"Подтвердить" -
    // ОБА локатора языкозависимы (ломаются, если интерфейс уже переключён на другой
    // язык), а сама задача метода ниже - переключать язык в ЛЮБУЮ сторону. Позиция
    // строки и кнопки на экране одинакова независимо от текущего языка - проверено
    // вживую 2026-08-29 (тап по этим же координатам сработал и из узбекского, и из
    // русского состояния). LANGUAGE_OPTION_UZBEK_LATIN/RUSSIAN выше - НЕ переведены,
    // это буквальные названия языков, поэтому остаются рабочими локаторами в обе
    // стороны без изменений.
    private static final int LANGUAGE_ROW_REF_X = 540;
    private static final int LANGUAGE_ROW_REF_Y = 320;
    private static final int LANGUAGE_DIALOG_CONFIRM_REF_X = 540;
    private static final int LANGUAGE_DIALOG_CONFIRM_REF_Y = 2237;

    // FAQ и Privacy policy открываются ВНУТРИ приложения (в отличие от "Yordam",
    // который открывает внешний Chrome - см. PROF-A03 в docs/exploration-notes.md).
    private static final By FAQ_FIRST_QUESTION = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Buyurtmani qanday rasmiylashtiraman\")");
    private static final By PRIVACY_DIALOG_TITLE = AppiumBy.accessibilityId("Maxfiylik siyosati");
    private static final By PRIVACY_CLOSE_BUTTON = AppiumBy.accessibilityId("Yopish");

    // SET-02: тумблер уведомлений - обнаружен вживую 2026-08-31 (отсутствовал в билде
    // 2026-08-27, см. javadoc SettingsTest). Сама строка - обычный кликабельный узел со
    // своим content-desc ("Xabarnomalarni yoqish"), но фактическое состояние (вкл/выкл) -
    // на СОСЕДНЕМ android.widget.Switch без content-desc (checkable=true, читаем через
    // атрибут "checked"), а не на самой строке. Клик по строке переключает Switch.
    private static final By NOTIFICATIONS_TOGGLE_ROW = AppiumBy.accessibilityId("Xabarnomalarni yoqish");
    private static final By NOTIFICATIONS_TOGGLE_SWITCH = AppiumBy.className("android.widget.Switch");

    // Включение тумблера срабатывает сразу, БЕЗ диалога - проверено вживую. Выключение,
    // наоборот, открывает отдельный диалог подтверждения ("Bildirishnomalarni
    // o'chirish" / "Haqiqatan bildirishnomalarni o'chirmoqchimisiz?" - "Bekor
    // qilish"/"O'chirish") - несимметричное поведение, обнаружено вживую 2026-08-31, нигде
    // раньше не документировано. Кнопки диалога - тот же паттерн, что и везде в проекте:
    // clickable="false" в дереве, но собственный (не смерженный) content-desc и реальные
    // bounds - находятся и кликаются по accessibility id несмотря на clickable="false"
    // (тот же случай, что CartScreen.CLEAR_CART_CONFIRM_BUTTON).
    private static final By DISABLE_NOTIFICATIONS_DIALOG_TITLE = AppiumBy.accessibilityId("Bildirishnomalarni o'chirish");
    private static final By DISABLE_NOTIFICATIONS_CANCEL = AppiumBy.accessibilityId("Bekor qilish");
    private static final By DISABLE_NOTIFICATIONS_CONFIRM = AppiumBy.accessibilityId("O'chirish");

    public SettingsScreen(AndroidDriver driver) {
        super(driver);
    }

    // SET-01 (базовая часть) / SET-05: строка языка, FAQ, политика конфиденциальности
    // и версия отображаются на экране настроек.
    public SettingsScreen verifyScreenShown() {
        Assert.assertTrue(waitFor(TIL_ROW).isDisplayed(), "Строка \"Til\" не отображается в Sozlamalar");
        Assert.assertTrue(driver.findElement(FAQ_ROW).isDisplayed(), "\"Ko'p beriladigan savollar\" не отображается");
        Assert.assertTrue(driver.findElement(PRIVACY_ROW).isDisplayed(), "\"Ilova maxfiylik siyosati\" не отображается");
        Assert.assertTrue(driver.findElement(VERSION_TEXT).isDisplayed(), "Версия приложения не отображается");
        return this;
    }

    // SET-01: открытие выбора языка показывает шторку с текущим языком и минимум
    // "O'zbekcha"/"Русский" среди опций. НЕ подтверждаем выбор (не тапаем Tasdiqlash) -
    // это осознанно лёгкая проверка без побочных эффектов на состояние приложения,
    // отдельно от полного цикла переключения (см. selectLanguage()/switchLanguageBackToUzbek()
    // ниже - это полноценный сценарий выбора+подтверждения+проверки закрепления,
    // доведённый и проверенный вживую 2026-08-29: старое опасение про откат языка при
    // повторном заходе в Sozlamalar не воспроизвелось 2/2 раза). Список языков в живой
    // проверке 2026-08-27 один раз показал 4 варианта (+ English), второй раз - только
    // 3 (без English) без каких-либо действий между проверками, поэтому English сюда не
    // включён как обязательный.
    public SettingsScreen verifyLanguagePickerShowsOptions() {
        waitFor(TIL_ROW).click();
        Assert.assertTrue(waitFor(LANGUAGE_DIALOG_TITLE).isDisplayed(), "Шторка \"Tilni tanlash\" не открылась");
        Assert.assertTrue(driver.findElement(LANGUAGE_OPTION_UZBEK_LATIN).isDisplayed(), "Опция \"O'zbekcha\" не отображается");
        Assert.assertTrue(driver.findElement(LANGUAGE_OPTION_RUSSIAN).isDisplayed(), "Опция \"Русский\" не отображается");
        tapAt(scaledX(LANGUAGE_DIALOG_CLOSE_REF_X), scaledY(LANGUAGE_DIALOG_CLOSE_REF_Y));
        Assert.assertTrue(waitFor(TIL_ROW).isDisplayed(), "Экран Sozlamalar не восстановился после закрытия шторки языка");
        return this;
    }

    // SET-03: FAQ открывается ВНУТРИ приложения (аккордеон вопросов), а не во внешнем
    // браузере.
    public SettingsScreen verifyFaqOpensInApp() {
        waitFor(FAQ_ROW).click();
        Assert.assertTrue(waitFor(FAQ_FIRST_QUESTION).isDisplayed(), "Список FAQ не отобразился");
        driver.navigate().back();
        Assert.assertTrue(waitFor(TIL_ROW).isDisplayed(), "Экран Sozlamalar не восстановился после FAQ");
        return this;
    }

    // SET-04: политика конфиденциальности открывается ВНУТРИ приложения (модалка с
    // текстом и кнопкой "Yopish"), а не во внешнем браузере.
    public SettingsScreen verifyPrivacyPolicyOpensInApp() {
        waitFor(PRIVACY_ROW).click();
        Assert.assertTrue(waitFor(PRIVACY_DIALOG_TITLE).isDisplayed(), "Модалка политики конфиденциальности не открылась");
        waitFor(PRIVACY_CLOSE_BUTTON).click();
        Assert.assertTrue(waitFor(TIL_ROW).isDisplayed(), "Экран Sozlamalar не восстановился после политики конфиденциальности");
        return this;
    }

    // SET-01: открывает шторку языка координатным тапом (работает независимо от
    // текущего языка интерфейса, см. LANGUAGE_ROW_REF_X/Y выше), выбирает опцию по
    // буквальному названию языка (не переведено, стабильно в обе стороны) и
    // подтверждает. Приложение само уводит на Home сразу после подтверждения -
    // проверено вживую 2026-08-29.
    public HomeScreen selectLanguage(By languageOptionLocator) {
        if (!tapLanguageRowUntilPickerOpens(languageOptionLocator)) {
            throw new IllegalStateException(
                    "Шторка \"Tilni tanlash\"/\"Выберите язык\" не открылась после нескольких попыток тапа по строке языка");
        }
        waitFor(languageOptionLocator).click();
        tapAt(scaledX(LANGUAGE_DIALOG_CONFIRM_REF_X), scaledY(LANGUAGE_DIALOG_CONFIRM_REF_Y));
        return new HomeScreen(driver);
    }

    // 2026-08-31, воспроизведено вживую дважды подряд: одиночный (и даже троекратный, без
    // ожидания стабилизации экрана) тап по координате строки языка не открывал шторку
    // выбора языка - LanguageSwitchTest словил это именно в finally-возврате на
    // узбекский, оставив РЕАЛЬНЫЙ аккаунт застрявшим на русском интерфейсе (все
    // остальные тесты проекта завязаны на узбекские локаторы - Hammasi/Bazar/Savat/...).
    // При этом ровно тот же тап по тем же координатам, сделанный вручную сразу ПОСЛЕ
    // того, как экран Sozlamalar/Настройки уже полностью отрисован (переход туда через
    // обычную навигацию, без спешки), срабатывал с первого раза. Похоже, проблема не в
    // самой координате, а в том, что предыдущий шаг (openSettingsRegardlessOfLanguage() -
    // координатный тап без waitFor) не ждёт, пока экран Sozlamalar реально долистает
    // переход/анимацию, прежде чем следующий координатный тап уйдёт в ещё не осевший
    // список. VERSION_TEXT ("V 1.1.8 (...)") - НЕ переведён и всегда последняя строка
    // экрана, поэтому его появление - надёжный языко-независимый сигнал того, что весь
    // экран Sozlamalar/Настройки уже отрисован, а не только его верхняя часть.
    private boolean tapLanguageRowUntilPickerOpens(By languageOptionLocator) {
        waitFor(VERSION_TEXT);
        sleep(Duration.ofMillis(300));
        int attemptsLeft = 3;
        while (attemptsLeft-- > 0) {
            tapAt(scaledX(LANGUAGE_ROW_REF_X), scaledY(LANGUAGE_ROW_REF_Y));
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(d -> !d.findElements(languageOptionLocator).isEmpty());
                return true;
            } catch (TimeoutException ignored) {
                // Шторка не открылась с этого тапа - пробуем ещё раз, пока не кончились попытки.
            }
        }
        return false;
    }

    // SET-01: подтверждает, что ВЫБРАННЫЙ язык закрепился (виден как текущее значение
    // строки языка) после повторного захода в Sozlamalar - именно это раньше
    // (2026-08-27) считалось нестабильным. descriptionContains по буквальному названию
    // языка, а не по переведённому лейблу строки ("Til"/"Язык") - см. LANGUAGE_ROW_REF
    // выше про то, почему сама строка не годится в качестве локатора здесь.
    public SettingsScreen verifyCurrentLanguageValueShown(String languageDisplayName) {
        By languageValue = AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + languageDisplayName + "\")");
        Assert.assertTrue(waitFor(languageValue).isDisplayed(),
                "Текущее значение языка \"" + languageDisplayName + "\" не отображается в Sozlamalar после повторного захода");
        return this;
    }

    // SET-02: включение - сразу, без диалога (позитивный сценарий); выключение требует
    // подтверждения в отдельном диалоге, а отмена в этом диалоге НЕ выключает тумблер
    // (негативный сценарий) - см. комментарий у DISABLE_NOTIFICATIONS_* выше. Метод
    // приводит тумблер к состоянию ВЫКЛ перед проверкой и оставляет его в состоянии ВЫКЛ
    // после (исходное состояние на реальном аккаунте на момент обнаружения фичи
    // 2026-08-31), независимо от того, в каком состоянии он был на входе.
    public SettingsScreen verifyNotificationsToggleEnableAndDisableWithConfirmation() {
        if (isNotificationsToggleEnabled()) {
            disableNotificationsToggleConfirmed();
        }
        Assert.assertFalse(isNotificationsToggleEnabled(), "Тумблер уведомлений не в состоянии ВЫКЛ перед началом проверки");

        waitFor(NOTIFICATIONS_TOGGLE_ROW).click();
        Assert.assertTrue(waitForToggleState(true), "Тумблер не включился по тапу (без диалога)");

        waitFor(NOTIFICATIONS_TOGGLE_ROW).click();
        Assert.assertTrue(waitFor(DISABLE_NOTIFICATIONS_DIALOG_TITLE).isDisplayed(),
                "Диалог подтверждения выключения уведомлений не открылся");
        waitFor(DISABLE_NOTIFICATIONS_CANCEL).click();
        Assert.assertTrue(waitForToggleState(true),
                "Отмена (\"Bekor qilish\") в диалоге выключения всё равно выключила тумблер");

        disableNotificationsToggleConfirmed();
        Assert.assertFalse(isNotificationsToggleEnabled(), "Тумблер не выключился после подтверждения (\"O'chirish\")");
        return this;
    }

    private void disableNotificationsToggleConfirmed() {
        waitFor(NOTIFICATIONS_TOGGLE_ROW).click();
        waitFor(DISABLE_NOTIFICATIONS_DIALOG_TITLE);
        waitFor(DISABLE_NOTIFICATIONS_CONFIRM).click();
        waitForToggleState(false);
    }

    private boolean isNotificationsToggleEnabled() {
        return Boolean.parseBoolean(waitFor(NOTIFICATIONS_TOGGLE_SWITCH).getAttribute("checked"));
    }

    private boolean waitForToggleState(boolean expectedChecked) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                    Boolean.parseBoolean(d.findElement(NOTIFICATIONS_TOGGLE_SWITCH).getAttribute("checked")) == expectedChecked);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
