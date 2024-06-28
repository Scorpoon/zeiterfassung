package de.focusshift.zeiterfassung.ui.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Paths;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@UsePlaywright(UiTest.CustomOptions.class)
@ExtendWith({ TestRecordVideoExtension.class })
@ContextConfiguration(initializers = UITestInitializer.class)
public @interface UiTest {

    class CustomOptions implements OptionsFactory {

        @Override
        public Options getOptions() {
            return new Options()
                .setConnectOptions(new BrowserType.ConnectOptions()
                    // increase to make test steps slower and be able to follow it with your own eyes.
                    .setSlowMo(200)
                )
                .setContextOptions(new Browser.NewContextOptions()
                    .setRecordVideoDir(Paths.get("target"))
                    .setLocale("de")
                    .setScreenSize(1500, 1080)
                    .setViewportSize(1500, 1080)
                );
        }
    }
}
