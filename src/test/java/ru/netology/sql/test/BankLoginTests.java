package ru.netology.sql.test;

import com.codeborne.selenide.SelenideElement;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.junit.jupiter.api.*;
import org.openqa.selenium.Keys;
import ru.netology.sql.data.DataHelper;
import ru.netology.sql.data.SQLHelper;
import ru.netology.sql.page.LoginPage;

import java.sql.SQLException;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static ru.netology.sql.data.SQLHelper.getConn;


public class BankLoginTests {
    private static final QueryRunner queryRunner = new QueryRunner();
    LoginPage loginPage;

    @AfterEach
    void tearDown() {
        SQLHelper.cleanAuthCodes();
    }
    @AfterAll
    static void tearDownAll() {
        SQLHelper.cleanDB();
    }
    @BeforeEach
    void setUp() {
        loginPage = open("http://localhost:9999/", LoginPage.class);
    }

    @Test
    void shouldSuccessLoginWithLoginAndPassword() {
        var authInfo = DataHelper.getAuthInfo();
        var verificationPage = loginPage.validLogin(authInfo);
        verificationPage.verificationPageVisible();
        var verificationCode = SQLHelper.getVerificationCode();
        verificationPage.validVerify(verificationCode.getCode());

    }
    @Test
    void shouldNotLoginWithUserDoesNotExist() {
        loginPage.validLogin(DataHelper.getRandomUser());
        loginPage.verificationErrorMassage("Ошибка! Неверно указан логин или пароль");
    }
    @Test
    void shouldNotLoginWithRandomVerificationCode() {
        var verificationPage = loginPage.validLogin(DataHelper.getAuthInfo());
        verificationPage.verificationPageVisible();
        verificationPage.verify(DataHelper.getRandomVerificationCode().getCode());
        verificationPage.verifyErrorNotification("Ошибка! Неверно указан код! Попробуйте ещё раз.");

    }
    @Test
    void shouldNotLoginWithWrongPassword() {
        loginPage.validLogin(DataHelper.getWrongPasswordAuthInfo());
        loginPage.verificationErrorMassage("Ошибка! Неверно указан логин или пароль");
    }
    @Test
    void shouldBlockUserIfThreeOrMoreLoginWithWrongPassword() throws SQLException {

        SelenideElement loginField = $("[data-test-id='login'] input");
        SelenideElement passwordField = $("[data-test-id='password'] input");

        loginPage.validLogin(DataHelper.getWrongPasswordAuthInfo());
        loginField.doubleClick().sendKeys(Keys.BACK_SPACE);
        passwordField.doubleClick().sendKeys(Keys.BACK_SPACE);

        loginPage.validLogin(DataHelper.getWrongPasswordAuthInfo());
        loginField.doubleClick().sendKeys(Keys.BACK_SPACE);
        passwordField.doubleClick().sendKeys(Keys.BACK_SPACE);

        loginPage.validLogin(DataHelper.getWrongPasswordAuthInfo());

        var codeSQL = "SELECT status FROM users WHERE login = 'vasya'";
        var conn = getConn();
        var status = queryRunner.query(conn, codeSQL, new ScalarHandler<String>());
        Assertions.assertEquals("blocked", status);
    }
}
