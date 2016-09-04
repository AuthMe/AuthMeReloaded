package fr.xephi.authme.command.executable.authme;

import ch.jalu.injector.Injector;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.converter.Converter;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link ConverterCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConverterCommandTest {

    @InjectMocks
    private ConverterCommand command;

    @Mock
    private CommandService commandService;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private Injector injector;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldHandleUnknownConversionType() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("invalid"));

        // then
        verify(sender).sendMessage(argThat(containsString("Converter does not exist")));
        verifyNoMoreInteractions(commandService);
        verifyZeroInteractions(injector);
        verifyZeroInteractions(bukkitService);
    }

    @Test
    public void shouldHaveUniqueClassForEachConverter() {
        // given
        Set<Class<? extends Converter>> classes = new HashSet<>();

        // when / then
        for (Map.Entry<String, Class<? extends Converter>> entry : ConverterCommand.CONVERTERS.entrySet()) {
            assertThat("Name is not null or empty",
                StringUtils.isEmpty(entry.getKey()), equalTo(false));

            assertThat("Converter class is unique for each entry",
                classes.add(entry.getValue()), equalTo(true));
        }
    }

    @Test
    public void shouldLaunchConverterForAllTypes() {
        // given
        String converterName = "rakamak";
        Class<? extends Converter> converterClass = ConverterCommand.CONVERTERS.get(converterName);
        Converter converter = createMockReturnedByInjector(converterClass);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(converterName));
        TestHelper.runInnerRunnable(bukkitService);

        // then
        verify(converter).execute(sender);
        verifyNoMoreInteractions(converter);
        verify(injector).newInstance(converterClass);
        verifyNoMoreInteractions(injector);
    }

    @Test
    public void shouldCatchExceptionInConverterAndInformSender() {
        // given
        String converterName = "vauth";
        Class<? extends Converter> converterClass = ConverterCommand.CONVERTERS.get(converterName);
        Converter converter = createMockReturnedByInjector(converterClass);
        doThrow(IllegalStateException.class).when(converter).execute(any(CommandSender.class));
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(converterName.toUpperCase()));
        TestHelper.runInnerRunnable(bukkitService);

        // then
        verify(converter).execute(sender);
        verifyNoMoreInteractions(converter);
        verify(injector).newInstance(converterClass);
        verifyNoMoreInteractions(injector);
        verify(commandService).send(sender, MessageKey.ERROR);
    }

    private <T extends Converter> T createMockReturnedByInjector(Class<T> clazz) {
        T converter = mock(clazz);
        given(injector.newInstance(clazz)).willReturn(converter);
        return converter;
    }

}
