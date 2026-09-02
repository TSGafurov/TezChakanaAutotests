package com.tezchakana.tests;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Подключает {@link RetryOnce} ко всем @Test методам сьюта глобально (см. <listeners>
 * в testng.xml/testng-safe.xml) - без этого пришлось бы дописывать
 * retryAnalyzer = RetryOnce.class на каждой из 34+ @Test аннотаций по отдельности и
 * не забывать делать то же для каждого нового теста.
 */
public class RetryAnalyzerTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryOnce.class);
    }
}
