package com.tezchakana.screens;

import com.tezchakana.config.TestConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Common gesture/wait helpers shared by every screen object, plus the
 * coordinate-tap workaround needed because this Flutter app merges several
 * interactive elements (bottom CTA button, bottom nav tabs) into a single
 * full-width/height accessibility node that doesn't expose a clickable
 * sub-element - locators taken from Appium Inspector XML page-source
 * snapshots.
 */
public abstract class BaseScreen {

    protected static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);

    // Экран, на котором сняты все координатные bounds ниже (см. Appium Inspector XML
    // снимки). Реальные тапы масштабируются под фактический размер экрана устройства
    // через scaledX()/scaledY().
    private static final int REFERENCE_SCREEN_WIDTH = TestConfig.referenceScreenWidth();
    private static final int REFERENCE_SCREEN_HEIGHT = TestConfig.referenceScreenHeight();

    // Нижняя красная CTA-кнопка ("To'lovga o'tish" / "Davom etish" / "Tasdiqlash" / ...)
    // смержена в один accessibility-узел на весь экран - click() тапает по ЦЕНТРУ экрана
    // (примерно y=1200), а не по самой кнопке внизу, поэтому промахивается. Изначально
    // снято как (540,2184) на экране 1080x2400, но при масштабировании под физическое
    // устройство 720x1600 промахивалось выше кнопки (видимо, из-за разной высоты
    // статус-бара/системной навигации между эталонным снимком и реальным устройством,
    // даже при одинаковом соотношении сторон экрана) - визуально подтверждено на
    // устройстве, что кнопка ближе к ~95% высоты экрана, скорректировано до 2286. Общий
    // паттерн повторяется на нескольких экранах, поэтому вынесен сюда.
    private static final int BOTTOM_CTA_REF_X = 540;
    private static final int BOTTOM_CTA_REF_Y = 2286;

    protected final AndroidDriver driver;

    protected BaseScreen(AndroidDriver driver) {
        this.driver = driver;
    }

    // Системный диалог "Location Accuracy" (пакет com.google.android.gms, НЕ часть
    // тестируемого приложения) может перекрыть экран в произвольный момент после
    // выдачи разрешения на геолокацию, не только сразу при переходе на Home - см.
    // HomeScreen (issue 11 в docs/exploration-notes.md). 2026-08-27: разовой проверки
    // перед returnToHomeScreen() оказалось недостаточно - диалог всплыл ПОСЛЕ того как
    // Home уже был найден, сорвав дальнейшие findElement на других экранах. Проверяем
    // на каждой итерации waitFor(), а не только в одном месте - иначе поймать момент
    // появления диалога невозможно.
    private static final By ROGUE_LOCATION_ACCURACY_DIALOG =
            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"No thanks\")");

    // Диалог подтверждения адреса при (пере)запуске приложения (см. issue 8 в
    // exploration-notes.md и HomeScreen.STARTUP_ADDRESS_DIALOG) - тот же класс риска,
    // что и ROGUE_LOCATION_ACCURACY_DIALOG выше: 2026-08-28 воспроизведено вживую, что
    // он может перекрыть совершенно ДРУГОЙ экран (Profile, не Home) в произвольный
    // момент между действиями, а не только сразу после запуска - сорвал find на
    // OrdersTest, никак не связанном с адресом. Проверяем на каждой итерации waitFor(),
    // а не только в HomeScreen.returnToHomeScreen(). Пропускаем проверку, если сам
    // ожидаемый locator - это диалог адреса (см. HomeScreen.verifyStartupAddressDialogShown,
    // ONB-04) - иначе тест, который специально ждёт появления этого диалога, никогда
    // его не увидит. "Ha, men shu yerdaman" - смерженный узел clickable=false с
    // реальными (не на весь экран) bounds, тап по координате центра, как и везде для
    // таких узлов; подтверждает уже активный адрес, ничего не меняет (тот же
    // безопасный тап, что уже используется в HomeScreen.confirmStartupAddress()).
    // SYS-01: раньше этот локатор и координаты были ЕЩЁ РАЗ независимо продублированы в
    // HomeScreen (используются там и для ONB-04, и внутри returnToHomeScreen()) - тот же
    // класс дублирования копий координат, который SYS-01 просит устранить. Здесь -
    // единственное определение, HomeScreen обращается к унаследованным
    // protected-членам вместо своей копии.
    protected static final By STARTUP_ADDRESS_DIALOG = AppiumBy.accessibilityId("Shu manzilga buyurtma berilsinmi?");
    private static final int STARTUP_CONFIRM_ADDRESS_REF_X = 540;
    private static final int STARTUP_CONFIRM_ADDRESS_REF_Y = 2069;

    // 2026-08-28, воспроизведено вживую: сначала эта проверка стояла БЕЗУСЛОВНО перед
    // каждым findElement (как ROGUE_LOCATION_ACCURACY_DIALOG выше) - лишний
    // findElements() на КАЖДОЙ итерации опроса, даже когда диалога нет вообще,
    // заметно замедлил поллинг и превратил на вид не связанный OrdersTest в
    // регулярно падающий по таймауту (первый прогон профиля после старта грузит
    // "Buyurtmalar" не мгновенно - лишний round-trip на каждой попытке съедал запас
    // из отведённых 15 секунд). Поэтому здесь порядок обратный: сначала ищем то, что
    // реально нужно тесту, и лишь если НЕ нашли - проверяем диалог как запасной
    // вариант, ничего не отнимая у "здорового" случая, когда диалога нет.
    protected WebElement waitFor(By locator) {
        return new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> {
                    clickIfPresent(ROGUE_LOCATION_ACCURACY_DIALOG, Duration.ofMillis(200));
                    List<WebElement> found = d.findElements(locator);
                    if (!found.isEmpty() && found.get(0).isDisplayed()) {
                        return found.get(0);
                    }
                    if (!locator.toString().contains("berilsinmi")) {
                        dismissStartupAddressDialogIfPresent();
                    }
                    return null;
                });
    }

    private void dismissStartupAddressDialogIfPresent() {
        if (!driver.findElements(STARTUP_ADDRESS_DIALOG).isEmpty()) {
            tapStartupAddressConfirm();
        }
    }

    // Тап по "Ha, men shu yerdaman" на диалоге подтверждения адреса - вынесено сюда,
    // чтобы HomeScreen (ONB-04, returnToHomeScreen()) не держал свою копию тех же
    // координат (см. SYS-01 в docs/exploration-notes.md).
    protected void tapStartupAddressConfirm() {
        tapAt(scaledX(STARTUP_CONFIRM_ADDRESS_REF_X), scaledY(STARTUP_CONFIRM_ADDRESS_REF_Y));
    }

    // 2026-08-28, воспроизведено вживую: закрытие диалога (например, "Yo'q" в
    // ORDH-03) не убирает узел из дерева доступности мгновенно - есть анимация
    // закрытия. Проверка isEmpty() сразу после тапа один раз поймала диалог
    // мид-анимации и ошибочно посчитала его всё ещё открытым, из-за чего тест не
    // просто дал ложный fail, а оставил РЕАЛЬНЫЙ диалог открытым на экране до конца
    // прогона (assert бросает исключение раньше, чем успевает закрыться) - это
    // унаследовала следующая сессия как нераспознанный экран, что в HomeScreen.
    // returnToHomeScreen() увело back-ом за пределы приложения. Ждём исчезновения
    // вместо мгновенной проверки.
    protected void waitUntilGone(By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(d -> d.findElements(locator).isEmpty());
    }

    protected boolean clickIfPresent(By locator, Duration timeout) {
        try {
            WebElement element = new WebDriverWait(driver, timeout)
                    .until(d -> {
                        List<WebElement> elements = d.findElements(locator);
                        return elements.isEmpty() ? null : elements.get(0);
                    });
            element.click();
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected void tapBottomCta() {
        tapAt(scaledX(BOTTOM_CTA_REF_X), scaledY(BOTTOM_CTA_REF_Y));
    }

    protected void tapAt(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofMillis(100)))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(tap));
    }

    protected void swipeUpOnScreen() {
        var size = driver.manage().window().getSize();
        int startX = size.width / 2;
        int startY = (int) (size.height * 0.8);
        int endY = (int) (size.height * 0.2);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofMillis(100)))
                .addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), startX, endY))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(swipe));
    }

    protected void typeViaAdb(String text) {
        try {
            new ProcessBuilder("adb", "shell", "input", "text", text).start().waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Не удалось ввести текст через adb: " + text, e);
        }
    }

    protected void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected int scaledX(int referenceX) {
        int actualWidth = driver.manage().window().getSize().width;
        return (int) Math.round(referenceX * ((double) actualWidth / REFERENCE_SCREEN_WIDTH));
    }

    protected int scaledY(int referenceY) {
        int actualHeight = driver.manage().window().getSize().height;
        return (int) Math.round(referenceY * ((double) actualHeight / REFERENCE_SCREEN_HEIGHT));
    }
}
