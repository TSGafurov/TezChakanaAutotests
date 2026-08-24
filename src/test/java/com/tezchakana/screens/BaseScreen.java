package com.tezchakana.screens;

import com.tezchakana.config.TestConfig;
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

    protected WebElement waitFor(By locator) {
        return new WebDriverWait(driver, WAIT_TIMEOUT)
                .until(d -> {
                    WebElement element = d.findElement(locator);
                    return element.isDisplayed() ? element : null;
                });
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
