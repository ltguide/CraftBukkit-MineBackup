package ltguide.minebackup.configuration;

import ltguide.minebackup.Base;
import ltguide.minebackup.data.Command;
import ltguide.minebackup.data.ICommand;
import ltguide.minebackup.data.IMessage;
import ltguide.minebackup.data.Message;

public class StringsConfiguration extends Configuration {
    private final IMessage[] messages;
    private final ICommand[] commands;

    public StringsConfiguration(final Base instance, final IMessage[] messages, final ICommand[] commands) {
        super(instance, "strings.yml");

        this.messages = messages;
        this.commands = commands;
    }

    @Override
    public void reload() {
        load();

        plugin.messagePrefix = plugin.colorize(getString("prefix")) + " ";

        for (final IMessage message : messages)
            plugin.messages.put(message.name(), new Message(plugin, message, getString("messages." + message.name().toLowerCase())));

        for (final ICommand command : commands) {
            final String path = "commands." + command.name().toLowerCase();

            plugin.commands.put(command.name(), new Command(plugin, command, getString(path + ".description"), get(path + ".broadcast")));
        }
    }
}
