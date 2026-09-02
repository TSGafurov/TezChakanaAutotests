package com.tezchakana.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Один повтор при падении - см. Known issue 9 в docs/exploration-notes.md: большие
 * пачки подряд идущих Appium-сессий деградируют эмулятор/UiAutomator2 и дают ~70%
 * ложных TimeoutException, которых нет ни при одиночном прогоне, ни в маленькой
 * группе. Это уже опознанная, задокументированная закономерность инфраструктуры, а
 * не маскировка настоящих багов - поэтому один автоматический повтор оправдан здесь
 * (в отличие от общего "ретраить, пока не позеленеет"). Второе падение подряд для
 * того же теста по-прежнему считается реальным провалом.
 *
 * Подключается ко всем @Test глобально через {@link RetryAnalyzerTransformer}, не
 * через retryAnalyzer= на каждой аннотации по отдельности.
 */
public class RetryOnce implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(RetryOnce.class);
    private static final int MAX_RETRIES = 1;

    private int retries = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retries >= MAX_RETRIES) {
            return false;
        }
        retries++;
        LOG.warn("Повтор {}/{} для {}.{}", retries, MAX_RETRIES,
                result.getTestClass().getRealClass().getSimpleName(), result.getMethod().getMethodName());
        return true;
    }
}
