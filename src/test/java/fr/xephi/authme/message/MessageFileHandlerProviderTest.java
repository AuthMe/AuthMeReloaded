package fr.xephi.authme.message;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import static fr.xephi.authme.TestHelper.getJarFile;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link MessageFileHandlerProvider}.
 */
@RunWith(DelayedInjectionRunner.class)
public class MessageFileHandlerProviderTest {

    private static final Function<String, String> MESSAGES_BUILDER = lang -> "messages/messages_" + lang + ".yml";

    @InjectDelayed
    private MessageFileHandlerProvider handlerProvider;

    @DataFolder
    private File dataFolder;
    @Mock
    private Settings settings;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
    public void initDataFolder() throws IOException {
        this.dataFolder = temporaryFolder.newFolder();
    }

    @Test
    public void shouldReturnExistingMessagesFile() {
        // given
        String language = "fr";
        // use another language file to make sure we won't copy over it
        String jarFile = "/messages/messages_it.yml";
        File existingFile = copyFromJar(MESSAGES_BUILDER.apply(language), jarFile);

        // when
        File result = handlerProvider.initializeFile(language, MESSAGES_BUILDER);

        // then
        assertThat(result, equalTo(existingFile));
        assertThat(result.exists(), equalTo(true));
        assertThat(result, equalToFile(getJarFile(jarFile)));
    }

    @Test
    public void shouldCopyFromJarFile() {
        // given
        String language = "nl";

        // when
        File result = handlerProvider.initializeFile(language, MESSAGES_BUILDER);

        // then
        File expectedFile = new File(dataFolder, MESSAGES_BUILDER.apply(language));
        assertThat(result, equalTo(expectedFile));
        assertThat(result.exists(), equalTo(true));
        assertThat(result, equalToFile(getJarFile("/messages/messages_nl.yml")));
    }

    @Test
    public void shouldCopyDefaultFileForUnknownLanguage() {
        // given
        String language = "zxx";

        // when
        File result = handlerProvider.initializeFile(language, MESSAGES_BUILDER);

        // then
        File expectedFile = new File(dataFolder, MESSAGES_BUILDER.apply(language));
        assertThat(result, equalTo(expectedFile));
        assertThat(result.exists(), equalTo(true));
        assertThat(result, equalToFile(getJarFile("/messages/messages_en.yml")));
    }

    @Test
    public void shouldReturnNullForNonExistentDefault() {
        // given
        Function<String, String> fileFunction = s -> "bogus";

        // when
        File result = handlerProvider.initializeFile("gsw", fileFunction);

        // then
        assertThat(result, nullValue());
    }

    @Test
    public void shouldCreateHandler() {
        // given
        String language = "fr";
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn(language);

        MessageFileHandlerProvider provider = Mockito.spy(handlerProvider);
        Function<String, String> fileFunction = lang -> "file_" + lang + ".txt";
        File file = new File(dataFolder, "some_file.txt");
        doReturn(file).when(provider).initializeFile(language, fileFunction);

        // when
        MessageFileHandler handler = provider.initializeHandler(fileFunction);

        // then
        assertThat(handler, not(nullValue()));
        verify(settings).getProperty(PluginSettings.MESSAGES_LANGUAGE);
        verify(provider).initializeFile(language, fileFunction);
    }

    private File copyFromJar(String path, String jarPath) {
        File file = new File(dataFolder, path);
        if (!file.getParentFile().mkdirs()) {
            throw new IllegalStateException("Could not create folders for '" + file + "'");
        }
        try {
            Files.copy(getJarFile(jarPath), file);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Matcher<File> equalToFile(File file) {
        return new TypeSafeMatcher<File>() {
            @Override
            protected boolean matchesSafely(File item) {
                try {
                    return Files.equal(item, file);
                } catch (IOException e) {
                    throw new IllegalStateException("IOException during matcher evaluation", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Equal to file '" + file + "'");
            }
        };
    }


}
