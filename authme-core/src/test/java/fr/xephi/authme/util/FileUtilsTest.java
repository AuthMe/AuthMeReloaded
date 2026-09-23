package fr.xephi.authme.util;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test for {@link FileUtils}.
 */
class FileUtilsTest {

    /** Regex that matches timestamps such as 20180211_1048. */
    private static final String BACKUP_TIMESTAMP_PATTERN = "20\\d{6}_\\d{4}";

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @TempDir
    File temporaryFolder;

    @Test
    void shouldNotCopyFile() throws IOException {
        // given
        File file = new File(temporaryFolder, "config.yml");
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
    void shouldCopyFileFromJar() {
        // given
        File file = new File(temporaryFolder, "some/folders/welcome.txt");

        // when
        boolean result = FileUtils.copyFileFromResource(file, "welcome.txt");

        // then
        assertThat(result, equalTo(true));
        assertThat(file.exists(), equalTo(true));
        File configJarFile = TestHelper.getJarFile("/welcome.txt");
        assertThat(file.length(), equalTo(configJarFile.length()));
    }

    @Test
    void shouldReturnFalseForInvalidJarFile() {
        // given
        File file = new File(temporaryFolder, "bogus");

        // when
        boolean result = FileUtils.copyFileFromResource(file, "does-not-exist");

        // then
        assertThat(result, equalTo(false));
        assertThat(file.exists(), equalTo(false));
    }

    @Test
    void shouldReturnFalseForParentInvalidParentFolders() throws IOException {
        // given
        new File(temporaryFolder, "hello").createNewFile();
        File fileToCreate = new File(temporaryFolder, "hello/test");

        // when
        boolean result = FileUtils.copyFileFromResource(fileToCreate, "welcome.txt");

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    void shouldPurgeDirectory() throws IOException {
        // given
        File file1 = new File(temporaryFolder, "a/b/c/test.html");
        File file2 = new File(temporaryFolder, "a/b/f/toast.txt");
        File file3 = new File(temporaryFolder, "a/g/rest.png");
        File file4 = new File(temporaryFolder, "j/l/roast.tiff");
        createFiles(file1, file2, file3, file4);

        // when
        FileUtils.purgeDirectory(new File(temporaryFolder, "a"));

        // then
        assertThat(file1.exists(), equalTo(false));
        assertThat(file2.exists(), equalTo(false));
        assertThat(file3.exists(), equalTo(false));
        assertThat(file4.exists(), equalTo(true));
        assertThat(new File(temporaryFolder, "a").exists(), equalTo(true));
    }

    @Test
    void shouldDeleteFile() throws IOException {
        // given
        File file = TestHelper.createFile(temporaryFolder, "tempFile");
        assertThat(file.exists(), equalTo(true));

        // when
        FileUtils.delete(file);

        // then
        assertThat(file.exists(), equalTo(false));
    }

    @Test
    void shouldDoNothingForNullFile() {
        // given
        File file = null;

        // when
        FileUtils.delete(file);

        // then
        // Nothing happens
    }

    @Test
    void shouldGetResourceFromJar() {
        // given / when / then
        assertThat(FileUtils.getResourceFromJar("config.yml"), not(nullValue()));
        assertThat(FileUtils.getResourceFromJar("does-not-exist"), nullValue());
    }

    @Test
    void shouldConstructPath() {
        // given/when
        String result = FileUtils.makePath("path", "to", "test-file.txt");

        // then
        assertThat(result, equalTo("path" + File.separator + "to" + File.separator + "test-file.txt"));
    }

    @Test
    void shouldCreateDirectory() {
        // given
        File dir = new File(temporaryFolder, "folder/folder2/myFolder");

        // when
        boolean result = FileUtils.createDirectory(dir);

        // then
        assertThat(result, equalTo(true));
        assertThat(dir.exists(), equalTo(true));
        assertThat(dir.isDirectory(), equalTo(true));
    }

    @Test
    void shouldReturnFalseOnDirectoryCreateFail() throws IOException {
        // given
        File dirAsFile = new File(temporaryFolder, "file");
        dirAsFile.createNewFile();

        // when
        boolean result = FileUtils.createDirectory(dirAsFile);

        // then
        assertThat(result, equalTo(false));
        assertThat(dirAsFile.isFile(), equalTo(true));
    }

    @Test
    void shouldCreateCurrentTimestampString() {
        // given / when
        String currentTimeString = FileUtils.createCurrentTimeString();

        // then
        assertThat(currentTimeString, matchesPattern(BACKUP_TIMESTAMP_PATTERN));
    }

    @Test
    void shouldCreateBackupFile() {
        // given
        File file = new File("some/folders/config.yml");

        // when
        String backupFile = FileUtils.createBackupFilePath(file);

        // then
        String folders = String.join(File.separator,"some", "folders", "").replace("\\", "\\\\");
        assertThat(backupFile, matchesPattern(folders + "backup_config_" + BACKUP_TIMESTAMP_PATTERN + "\\.yml"));
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
