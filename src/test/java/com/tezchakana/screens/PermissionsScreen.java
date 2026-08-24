package com.tezchakana.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

public class PermissionsScreen extends BaseScreen {

    public PermissionsScreen(AndroidDriver driver) {
        super(driver);
    }

    public PermissionsScreen handleNotificationsPermission() {
        By allowButton = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button");
        clickIfPresent(allowButton, Duration.ofSeconds(10));
        return this;
    }

    // Захваченный диалог предлагает только "While using the app" / "Only this time" /
    // "Don't allow" - варианта "Allow all the time" в этом снимке нет.
    public PaymentScreen handleLocationPermission() {
        By whileUsingApp = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_foreground_only_button");
        clickIfPresent(whileUsingApp, Duration.ofSeconds(10));
        return new PaymentScreen(driver);
    }
}
