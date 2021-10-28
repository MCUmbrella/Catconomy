package ninekothecat.catconomy.commands.catconomycommand;

import ninekothecat.catconomy.Catconomy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CatEconomyCommandHandlerAutoCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(Catconomy.catEconomyCommandHandler.getCommands().keySet());
        } else if (args.length > 1 && Catconomy.catEconomyCommandHandler.getCommands().containsKey(args[0]) && Catconomy.catEconomyCommandHandler.get(args[0]).hasAutoComplete()) {
            List<String> arg2 = new ArrayList<>(Arrays.asList(args));
            arg2.remove(0);
            String[] args3 = arg2.toArray(new String[args.length - 1]);
            return Catconomy.catEconomyCommandHandler.get(args[0]).getAutoComplete().onTabComplete(sender, command, alias, args3);
        } else {
            return null;
        }

    }
}