package fr.xephi.authme.command.executable.authme;

import ch.jalu.injector.factory.Factory;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.converter.Converter;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskAsynchronously;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link ConverterCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConverterCommandTest {

    @InjectMocks
    private ConverterCommand command;

    @Mock
    private CommonService commonService;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private Factory<Converter> converterFactory;

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
        String converters = String.join(", ", ConverterCommand.CONVERTERS.keySet());
        verify(sender).sendMessage(argThat(containsString(converters)));
        verifyNoInteractions(commonService, converterFactory, bukkitService);
    }

    @Test
    public void shouldHandleCommandWithNoArgs() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        String converters = String.join(", ", ConverterCommand.CONVERTERS.keySet());
        verify(sender).sendMessage(argThat(containsString(converters)));
        verifyNoInteractions(commonService, converterFactory, bukkitService);
    }

    @Test
    public void shouldHaveUniqueClassForEachConverter() {
        // given
        Set<Class<? extends Converter>> classes = new HashSet<>();

        // when / then
        for (Map.Entry<String, Class<? extends Converter>> entry : ConverterCommand.CONVERTERS.entrySet()) {
            assertThat("Name is not null or empty",
                StringUtils.isBlank(entry.getKey()), equalTo(false));

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
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, Collections.singletonList(converterName));

        // then
        verify(converter).execute(sender);
        verifyNoMoreInteractions(converter);
        verify(converterFactory).newInstance(converterClass);
        verifyNoMoreInteractions(converterFactory);
    }

    @Test
    public void shouldCatchExceptionInConverterAndInformSender() {
        // given
        String converterName = "vauth";
        Class<? extends Converter> converterClass = ConverterCommand.CONVERTERS.get(converterName);
        Converter converter = createMockReturnedByInjector(converterClass);
        doThrow(IllegalStateException.class).when(converter).execute(any(CommandSender.class));
        CommandSender sender = mock(CommandSender.class);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, Collections.singletonList(converterName.toUpperCase(Locale.ROOT)));

        // then
        verify(converter).execute(sender);
        verifyNoMoreInteractions(converter);
        verify(converterFactory).newInstance(converterClass);
        verifyNoMoreInteractions(converterFactory);
        verify(commonService).send(sender, MessageKey.ERROR);
    }

    private <T extends Converter> T createMockReturnedByInjector(Class<T> clazz) {
        T converter = mock(clazz);
        given(converterFactory.newInstance(clazz)).willReturn(converter);
        return converter;
    }

}
