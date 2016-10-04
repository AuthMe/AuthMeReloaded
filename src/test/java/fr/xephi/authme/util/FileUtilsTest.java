package fr.xephi.authme.util;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link FileUtils}.
 */
public class FileUtilsTest {

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldNotCopyFile() throws IOException {
        // given
        File folder = temporaryFolder.newFolder();
        File file = new File(folder, "config.yml");
        // purposely don't copy config.yml to verify that config.yml isn't copied by the method
        File emailJarFile = TestHelper.getJarFile("/email.html");
        Files.copy(emailJarFile, file);

        // when
        boolean result = FileUtils.copyFileFromResource(file, "config.yml");

        // then
        assertThat(result, equalTo(true));
        assertThat(file.length(), equalTo(emailJarFile.length()));
    }

    @Test
    public void shouldCopyFileFromJar() throws IOException {
        // given
        File folder = temporaryFolder.newFolder();
        File file = new File(folder, "some/folders/config.yml");

        // when
        boolean result = FileUtils.copyFileFromResource(file, "config.yml");

        // then
        assertThat(result, equalTo(true));
        assertThat(file.exists(), equalTo(true));
        File configJarFile = TestHelper.getJarFile("/config.yml");
        assertThat(file.length(), equalTo(configJarFile.length()));
    }

    @Test
    public void shouldReturnFalseForInvalidJarFile() throws IOException {
        // given
        File folder = temporaryFolder.newFolder();
        File file = new File(folder, "bogus");

        // when
        boolean result = FileUtils.copyFileFromResource(file, "does-not-exist");

        // then
        assertThat(result, equalTo(false));
        assertThat(file.exists(), equalTo(false));
    }

    @Test
    public void shouldPurgeDirectory() throws IOException {
        // given
        File root = temporaryFolder.newFolder();
        File file1 = new File(root, "a/b/c/test.html");
        File file2 = new File(root, "a/b/f/toast.txt");
        File file3 = new File(root, "a/g/rest.png");
        File file4 = new File(root, "j/l/roast.tiff");
        createFiles(file1, file2, file3, file4);

        // when
        FileUtils.purgeDirectory(new File(root, "a"));

        // then
        assertThat(file1.exists(), equalTo(false));
        assertThat(file2.exists(), equalTo(false));
        assertThat(file3.exists(), equalTo(false));
        assertThat(file4.exists(), equalTo(true));
        assertThat(new File(root, "a").exists(), equalTo(true));
    }

    @Test
    public void shouldDeleteFile() throws IOException {
        // given
        File file = temporaryFolder.newFile();
        assertThat(file.exists(), equalTo(true));

        // when
        FileUtils.delete(file);

        // then
        assertThat(file.exists(), equalTo(false));
    }

    @Test
    public void shouldDoNothingForNullFile() {
        // given
        File file = null;

        // when
        FileUtils.delete(file);

        // then
        // Nothing happens
    }

    @Test
    public void shouldConstructPath() {
        // given/when
        String result = FileUtils.makePath("path", "to", "test-file.txt");

        // then
        assertThat(result, equalTo("path" + File.separator + "to" + File.separator + "test-file.txt"));
    }

    private static void createFiles(File... files) throws IOException {
        for (File file : files) {
            boolean result = file.getParentFile().mkdirs() & file.createNewFile();
            if (!result) {
                throw new IllegalStateException("Cannot create file '" + file + "'");
            }
        }
    }

}
