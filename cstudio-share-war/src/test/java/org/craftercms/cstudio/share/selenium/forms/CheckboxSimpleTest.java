/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.cstudio.share.selenium.forms;

import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.ie.*;
import org.openqa.selenium.support.ui.*;
import org.craftercms.cstudio.share.selenium.basic.*;

import static org.junit.Assert.*;

public class CheckboxSimpleTest {
  @Ignore
  @Test
  public void test_on_internet_explorer() {
    test_checkbox_simple(new InternetExplorerDriver());
  }

  @Test
  public void test_on_firefox() {
    test_checkbox_simple(new FirefoxDriver());
  }

  @Ignore
  @Test
  public void test_on_chrome() {
    test_checkbox_simple(new ChromeDriver());
  }

  private void test_checkbox_simple(WebDriver driver) {
    // Login
    CStudioSeleniumUtil.try_login(driver, CStudioSeleniumUtil.AUTHOR_USER, CStudioSeleniumUtil.AUTHOR_PASSWORD, true);

    // Navigate to Widget
    CStudioSeleniumUtil.navigate_to_checkbox_simple_widget(driver);

    // Wait until checkboxes are rendered
    new WebDriverWait(driver, CStudioSeleniumUtil.SHORT_TIMEOUT).until(new ExpectedCondition<Boolean>() {
      public Boolean apply(WebDriver d) {
        return d.findElement(By.tagName("body")).getText().contains("Default True");
      }
    });

    // Test hint
    CStudioSeleniumUtil.click_on(driver, By.id("xf-4$xf-11$checkEmptyId$xf-188$$i"));
    CStudioSeleniumUtil.wait_until_displayed(driver, CStudioSeleniumUtil.SHORT_TIMEOUT, By.id("yui-gen5_c"));
    CStudioSeleniumUtil.click_on(driver, By.xpath("/html/body/div/div/div/div/div/div[2]/form/div[2]/div/a"));
    CStudioSeleniumUtil.wait_until_not_displayed(driver, CStudioSeleniumUtil.SHORT_TIMEOUT, By.id("yui-gen5_c"));

    // Test hint
    CStudioSeleniumUtil.click_on(driver, By.id("xf-4$xf-11$checkTrueId$xf-198$$i"));
    CStudioSeleniumUtil.wait_until_displayed(driver, CStudioSeleniumUtil.SHORT_TIMEOUT, By.id("yui-gen5_c"));
    CStudioSeleniumUtil.click_on(driver, By.xpath("/html/body/div/div/div/div/div/div[2]/form/div[2]/div/a"));
    CStudioSeleniumUtil.wait_until_not_displayed(driver, CStudioSeleniumUtil.SHORT_TIMEOUT, By.id("yui-gen5_c"));

    // Test checkbox value
    WebElement element = driver.findElement(By.id("xf-4$xf-11$checkEmptyId$checkbox-simple$$e0"));
    assertFalse(element.isSelected());
    element.click();
    assertTrue(element.isSelected());

    // Test checkbox value
    element = driver.findElement(By.id("xf-4$xf-11$checkTrueId$checkbox-simple$$e0"));
    assertTrue(element.isSelected());
    element.click();
    assertFalse(element.isSelected());

    // Close driver
    CStudioSeleniumUtil.exit(driver);
  }
}
