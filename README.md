# TezChakana Autotests

Appium + TestNG UI-автотесты для Android-приложения "Tez Chakana" (`uz.agrobank.chakanaexpress`),
Page Object Model (`screens/` + `BaseScreen` + `TestConfig`).

## ⚠️ Реальный аккаунт на живом бэкенде

Сьют работает против **реального личного аккаунта владельца проекта на живом продовом
бэкенде**, не изолированного QA-стенда. На нём реальные заказы, адреса, сохранённые
карты; оформление заказа списывает реальные деньги (возвратно через отмену в
приложении). Подробности, известные баги приложения и полная матрица покрытия тест-кейсов -
в [`docs/exploration-notes.md`](docs/exploration-notes.md).

Прежде чем менять `phone.number`/`otp.code` или включать `OrderErrorTest`/`OrderRetryTest`
обратно в сьют - согласуй с владельцем.

## Требования

- Java 21, Maven
- Запущенный Android-эмулятор или устройство, видимое в `adb devices`
- Локальный Appium-сервер (по умолчанию `http://127.0.0.1:4723`, см. `appium.url`)
- Установленное приложение `uz.agrobank.chakanaexpress`

## Настройка

Тестовые данные читаются из `src/test/resources/config.properties` - этот файл **не
хранится в git** (см. `.gitignore`), т.к. содержит реальный номер телефона и OTP
реального аккаунта. Перед первым запуском:

```bash
cp src/test/resources/config.properties.example src/test/resources/config.properties
# заполнить phone.number/otp.code реальными значениями
```

Либо не трогать файл вообще и передавать значения флагами:

```bash
mvn test -Dphone.number=... -Dotp.code=...
```

Любой ключ из `config.properties` можно переопределить так же, через `-D<key>=...`
(см. `TestConfig.get()`).

## Запуск

Полный регресс (safe + mutating тесты, вручную, с осторожностью - см. ниже):

```bash
mvn test
```

Только безопасное read-only подмножество (без мутации реального аккаунта, годится для
автоматического/CI прогона):

```bash
mvn test -DsuiteXmlFile=src/test/resources/testng-safe.xml
```

## Уровни риска тестов

Каждый `@Test`-метод помечен группой TestNG:

| Группа | Значение | Где смотреть |
|---|---|---|
| `safe` | Read-only, не меняет реальный аккаунт | `testng-safe.xml` |
| `mutating` | Меняет реальные данные (корзину/адрес/язык/уведомления), но обратимо и без денег | `testng.xml` (полный регресс) |
| `destructive` | Может создать реальный заказ | `OrderErrorTest`/`OrderRetryTest` - **не подключены** ни в один сьют, см. комментарий в `testng.xml` |

## При падении теста

- `ScreenshotOnFailureListener` (подключён глобально через `<listeners>` в обоих сьютах)
  сохраняет скриншот устройства в `target/screenshots/<Класс>.<метод>-<таймстамп>.png` в
  момент падения - не нужно вслепую перезапускать сценарий на устройстве, чтобы понять,
  на каком экране всё пошло не так.
- `RetryAnalyzerTransformer`/`RetryOnce` дают один автоматический повтор любому упавшему
  тесту - см. Known issue 9 в `docs/exploration-notes.md` про то, почему это оправдано
  здесь (деградация эмулятора в больших пачках сессий, а не маскировка реальных багов).
  Второе падение подряд считается настоящим провалом.
- Полный лог прогона (preflight-чеклист, ретраи, сохранённые скриншоты) - в
  `target/logs/tests.log` (Logback, см. `src/test/resources/logback.xml`), не только в
  консоли Maven.

## Структура

- `src/test/java/com/tezchakana/screens/` - Page Object Model (по одному классу на экран)
- `src/test/java/com/tezchakana/tests/` - тестовые классы (`BaseTest` - общий setup/teardown)
- `src/test/java/com/tezchakana/config/TestConfig.java` - типизированный доступ к `config.properties`
- `src/test/resources/testng.xml` / `testng-safe.xml` - сьюты
- `docs/exploration-notes.md` - живая матрица покрытия тест-кейсов + известные баги приложения
