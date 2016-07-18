package fr.xephi.authme.command.executable.authme;

import ch.jalu.injector.Injector;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.converter.RakamakConverter;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
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

    @Test
    public void shouldHandleUnknownConversionType() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("invalid"));

        // then
        verify(commandService).send(sender, MessageKey.ERROR);
        verifyNoMoreInteractions(commandService);
        verifyZeroInteractions(injector);
        verifyZeroInteractions(bukkitService);
    }

    @Test
    public void shouldHaveUniqueNameAndClassForEachType() {
        // given
        ConverterCommand.ConvertType[] types = ConverterCommand.ConvertType.values();
        List<String> names = new ArrayList<>(types.length);
        List<Class<?>> classes = new ArrayList<>(types.length);

        // when / then
        for (ConverterCommand.ConvertType type : types) {
            assertThat("Name for '" + type + "' is not null",
                type.getName(), not(nullValue()));
            assertThat("Class for '" + type + "' is not null",
                type.getConverterClass(), not(nullValue()));
            assertThat("Name '" + type.getName() + "' is unique",
                names, not(hasItem(type.getName())));
            assertThat("Class '" + type.getConverterClass() + "' is unique",
                classes, not(hasItem(type.getConverterClass())));
            names.add(type.getName());
            classes.add(type.getConverterClass());
        }
    }

    @Test
    public void shouldLaunchConverterForAllTypes() {
        // given
        ConverterCommand.ConvertType type = ConverterCommand.ConvertType.RAKAMAK;
        RakamakConverter converter = mock(RakamakConverter.class);
        given(injector.newInstance(RakamakConverter.class)).willReturn(converter);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(type.getName()));
        TestHelper.runInnerRunnable(bukkitService);

        // then
        verify(converter).execute(sender);
        verifyNoMoreInteractions(converter);
        verify(injector).newInstance(type.getConverterClass());
        verifyNoMoreInteractions(injector);
    }

}
