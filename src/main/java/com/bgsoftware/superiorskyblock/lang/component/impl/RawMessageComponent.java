package com.bgsoftware.superiorskyblock.lang.component.impl;

import com.bgsoftware.superiorskyblock.lang.component.EmptyMessageComponent;
import com.bgsoftware.superiorskyblock.lang.component.IMessageComponent;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.command.CommandSender;

import javax.annotation.Nullable;

public final class RawMessageComponent implements IMessageComponent {

    private final String message;

    public static IMessageComponent of(@Nullable String message) {
        return Strings.isBlank(message) ? EmptyMessageComponent.getInstance() : new RawMessageComponent(message);
    }

    private RawMessageComponent(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public void sendMessage(CommandSender sender, Object... objects) {
        IMessageComponent.replaceArgs(this.message, objects).ifPresent(sender::sendMessage);
    }

}