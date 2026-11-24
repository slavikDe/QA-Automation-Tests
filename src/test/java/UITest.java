import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.util.List;

import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UITest {
    private final String VALID_USERNAME = "standard_user";
    private final String VALID_PASSWORD = "secret_sauce";

    public enum SortingOrder {
        NAME_ASC("az"),
        NAME_DES("za"),
        PRICE_ASC("lohi"),
        PRICE_DES("hilo");

        private String value;
        SortingOrder(String value) {
            this.value = value;
        }
        public String value() { return this.value; }
    }

    @BeforeEach
    void openSite() {
        open("https://www.saucedemo.com");
    }

    @Test
    @DisplayName("Standard user logs in and sees the Products page with products")
    public void successfulLoginOpensProductsPage() {
        login(VALID_USERNAME, VALID_PASSWORD);
        shouldBeOnProductsPage();

        $$(".inventory_item").shouldHave(sizeGreaterThan(0));
        $(".inventory_item").shouldBe(visible);
    }

    @Test
    @DisplayName("Sorted product can be added to the cart and is shown there")
    public void sortAndAddProductToCart() {
        login(VALID_USERNAME, VALID_PASSWORD);

        shouldBeOnProductsPage();
        applyOrderSorting(SortingOrder.PRICE_ASC);
        verifyPricesSortedAscending();

        // add product to the cart
        String productName = addAnyProductToCart();

        // open cart
        shouldBeOnProductsPage();
        $("#shopping_cart_container").$("a").click();

        shouldBeOnCartPage();

        assertEquals(1, $$(".cart_item").size());
        $("[data-test='inventory-item-name']").shouldHave(exactText(productName));
    }

    private String addAnyProductToCart() {
        SelenideElement product = $$("[data-test='inventory-item']")
                .shouldHave(sizeGreaterThan(0))
                .first();

        String name = product.$("[data-test='inventory-item-name']").getText();
        product.$("button").shouldHave(text("Add to cart")).click();
        product.$("button").shouldHave(text("Remove"));
        return name;
    }

    private void verifyPricesSortedAscending() {
        List<Double> prices = $$("[data-test='inventory-item-price']")
                .shouldHave(sizeGreaterThan(0))
                .texts().stream()
                .map(text -> Double.parseDouble(text.replace("$", "").trim()))
                .toList();

        List<Double> sorted = prices.stream().sorted().toList();
        assertEquals(sorted, prices, "Prices are not sorted ascending");
    }

    private void applyOrderSorting(SortingOrder order) {
        $("[data-test=\"product-sort-container\"]").selectOptionByValue(order.value);
    }

    private void shouldBeOnProductsPage() {
        webdriver().shouldHave(urlContaining("/inventory.html"));
    }

    private void shouldBeOnCartPage() {
        webdriver().shouldHave(urlContaining("/cart.html"));
    }

    private void login(String username, String password) {
        $(By.name("user-name")).setValue(username);
        $(By.name("password")).setValue(password);
        $("#login-button").click();
    }
}
