package ltguide.base.configuration;

import ltguide.base.Base;
import ltguide.base.data.Command;
import ltguide.base.data.ICommand;
import ltguide.base.data.IMessage;
import ltguide.base.data.Message;

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
