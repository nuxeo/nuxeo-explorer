/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.pages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.nuxeo.apidoc.browse.Distribution;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * Page representing the administration of distributions
 *
 * @since 11.1
 */
public class DistribAdminPage extends AbstractExplorerPage {

    public static final String URL = String.format("%s%s/", ExplorerHomePage.URL, Distribution.VIEW_ADMIN);

    /** @since 20.0.0 */
    public static final String UPDATE_URL = String.format("%s%s/", ExplorerHomePage.URL, Distribution.UPDATE_ACTION);

    /** @since 20.0.0 */
    public static final String DELETE_URL = String.format("%s%s/", ExplorerHomePage.URL, Distribution.DELETE_ACTION);

    protected static final String DUPLICATE_KEY_CLASS = "duplicateKey";

    @Required
    @FindBy(xpath = "//h1")
    public WebElement distributionsTitle;

    @FindBy(linkText = "UPDATE")
    public WebElement firstUpdateLink;

    @FindBy(linkText = "EXPORT AS ZIP")
    public WebElement firstExportLink;

    @FindBy(linkText = "DELETE")
    public WebElement firstDeleteLink;

    public DistribAdminPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void check() {
        checkTitle("Nuxeo Platform Explorer");
        UploadFragment.checkCanSee();
        assertTrue(driver.findElements(By.cssSelector("div." + DUPLICATE_KEY_CLASS)).isEmpty());
        assertFalse(driver.getPageSource().contains("Duplicate key detected"));
    }

    /**
     * Saves current live distribution with given name and returns the version.
     */
    public String saveCurrentLiveDistrib(String newName, boolean partial, boolean includeReferences) {
        clickOn(driver.findElement(By.id(partial ? "savePartial" : "save")));
        WebElement div = driver.findElement(By.id(partial ? "extendedSave" : "stdSave"));
        if (newName != null) {
            WebElement nameInput = div.findElement(By.xpath(".//input[@name='name']"));
            nameInput.clear();
            nameInput.sendKeys(newName);
        }
        if (partial) {
            WebElement bundlesInput = div.findElement(By.xpath(".//textarea[@name='bundles']"));
            bundlesInput.sendKeys("org.nuxeo.apidoc");
        }
        if (includeReferences) {
            WebElement includeInput = div.findElement(By.xpath(".//input[@name='includeReferences']"));
            if (!includeInput.isSelected()) {
                Locator.scrollAndForceClick(includeInput);
            }
        }
        String version = div.findElement(By.xpath(".//span[@name='version']")).getText();
        clickOn(driver.findElement(By.id(partial ? "doSaveExtended" : "doSave")));
        waitForAsyncWork();
        By continueBy = By.linkText("CONTINUE");
        Locator.waitUntilElementPresent(continueBy);
        Locator.waitUntilEnabledAndClick(driver.findElement(continueBy));
        return version;
    }

    /**
     * Exports first persisted distribution and returns corresponding file.
     */
    public File exportFirstPersistedDistrib(File downloadDir, String filename) {
        clickOn(firstExportLink);
        File export = new File(downloadDir, filename);
        // wait for download to happen
        Wait<WebDriver> wait = getLongWait();
        wait.until((new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    return export.exists();
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            }
        }));
        return export;
    }

    public DistributionUpdatePage updateFirstPersistedDistrib() {
        clickOn(firstUpdateLink);
        return asPage(DistributionUpdatePage.class);
    }

    /**
     * Deletes first persisted distribution.
     *
     * @since 20.0.0
     */
    public void deleteFirstPersistedDistrib() {
        clickOn(firstDeleteLink);
        Alert confirmRemove = driver.switchTo().alert();
        confirmRemove.accept();
        waitForAsyncWork();
        Locator.waitUntilElementPresent(By.id("successMessage"));
        checkSuccessMessage("Deletion Done.");
    }

    public void checkCanSave() {
        assertTrue(driver.findElement(By.id("savePartial")).isEnabled());
        assertTrue(driver.findElement(By.id("save")).isEnabled());
    }

    public void checkCannotSave() {
        // check we're an explorer page still
        checkTitle("Nuxeo Platform Explorer");
        try {
            driver.findElement(By.id("savePartial"));
            fail("Should not be able to save partial");
        } catch (NoSuchElementException e) {
        }
        try {
            driver.findElement(By.id("save"));
            fail("Should not be able to save");
        } catch (NoSuchElementException e) {
        }
    }

    public void checkDuplicateKeyErrorMessages(String... messages) {
        List<WebElement> elements = driver.findElements(By.cssSelector("div." + DUPLICATE_KEY_CLASS));
        assertEquals(messages.length, elements.size());
        for (int i = 0; i < messages.length; i++) {
            assertEquals(messages[i], elements.get(i).getText());
        }
    }

}
