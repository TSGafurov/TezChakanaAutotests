package com.tezchakana.tests;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Скриншот устройства в момент падения теста - раньше единственным способом понять
 * причину было перезапустить тот же сценарий вживую на устройстве (так велось
 * расследование багов returnToHomeScreen()/OrdersTest, см. project_app_state_gotcha_
 * after_manual_exploration в памяти проекта). Подключается глобально через
 * &lt;listeners&gt; в testng.xml/testng-safe.xml, а не через @Listeners на каждом
 * классе - работает для любого теста, унаследованного от BaseTest, без правки самих
 * тестов.
 */
public class ScreenshotOnFailureListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotOnFailureListener.class);
    private static final Path SCREENSHOT_DIR = Path.of("target", "screenshots");
    private static final DateTimeFormatter TIMESTAMP_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void onTestFailure(ITestResult result) {
        Object instance = result.getInstance();
        // Не каждый провал - это Appium-сессия (например, падение до создания driver
        // в BaseTest.setUp()) - в этом случае просто нечего снимать.
        if (!(instance instanceof BaseTest baseTest) || baseTest.driver == null) {
            return;
        }

        try {
            Files.createDirectories(SCREENSHOT_DIR);
            File source = ((TakesScreenshot) baseTest.driver).getScreenshotAs(OutputType.FILE);
            String fileName = result.getTestClass().getRealClass().getSimpleName()
                    + "." + result.getMethod().getMethodName()
                    + "-" + LocalDateTime.now().format(TIMESTAMP_PATTERN) + ".png";
            Path target = SCREENSHOT_DIR.resolve(fileName);
            Files.copy(source.toPath(), target);
            LOG.info("Сохранён скриншот падения: {}", target);
        } catch (IOException | RuntimeException e) {
            // Скриншот - диагностика, а не часть самого теста: падение здесь не должно
            // маскировать/заменять исходную причину провала теста.
            LOG.warn("Не удалось сохранить скриншот: {}", e.getMessage());
        }
    }
}
