package wustendorf;

import net.minecraft.src.*;

import java.util.*;

@SuppressWarnings("unchecked")
public class CommandWustendorf extends CommandBase {
    public String getCommandName()
    {
        return "w";
    }

    /**
     * Return the required permission level for this command.
     */
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    public String getCommandUsage(ICommandSender sender)
    {
        return "/w help";
    }

    public void processCommand(ICommandSender sender, String[] params)
    {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendChatToPlayer("Not available from console.");
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;

        if (!(player.worldObj instanceof WorldServer)) {
            // WTF?  This is a server command!
            sender.sendChatToPlayer("World not sane.");
            return;
        }

        WorldServer world = (WorldServer) player.worldObj;

        if (params.length < 1 || params[0].equals("help")) {
            if (params.length < 2) {
                sender.sendChatToPlayer("Available subcommands:");
                sender.sendChatToPlayer("tag, range");
            } else if (params[1].equals("tag")) {
                sender.sendChatToPlayer("Usage:");
                sender.sendChatToPlayer("/w tag {|list} - List tags on target flag.");
                sender.sendChatToPlayer("/w tag <tag> [level] - Read or set tag on target flag.");
                sender.sendChatToPlayer("/w tag clear <tag> - Remove tag from target flag.");
            } else if (params[1].equals("range")) {
                sender.sendChatToPlayer("Usage:");
                sender.sendChatToPlayer("/w range - Show range of target flag.");
                sender.sendChatToPlayer("/w range <blocks> - Set range of target flag.");
            } else {
                sender.sendChatToPlayer("Unknown subcommand.");
            }

            return;
        }

        // Flag-bound commands.

        Vec3 position = world.getWorldVec3Pool().getVecFromPool(player.posX, player.posY, player.posZ);
        position.yCoord += 1.6;
        Vec3 look = player.getLook(0F);
        Vec3 lookLimit = position.addVector(look.xCoord * 10, look.yCoord * 10, look.zCoord * 10);
        MovingObjectPosition hit = world.rayTraceBlocks(position, lookLimit);

        boolean good = false;
        int x=0, y=0, z=0;
        if (hit != null && hit.typeOfHit == EnumMovingObjectType.TILE) {
            x = hit.blockX;
            y = hit.blockY;
            z = hit.blockZ;
            int id = world.getBlockId(x, y, z);
            Block block = Block.blocksList[id];

            if (block instanceof WustendorfMarker) {
                good = true;
            }
        }

        if (!good) {
            sender.sendChatToPlayer("That's not a house flag.");

            return;
        }

        WustendorfDB worldDB = Wustendorf.getWorldDB(world);
        DBMarker marker = worldDB.getMarkerAt(x, y, z);

        if (params[0].equals("tag")) {
            if (params.length < 2 || params[1].equals("list")) {
                Map<String, Integer> tags = marker.getAllTags();

                if (tags == null) {
                    sender.sendChatToPlayer("This house flag is uninitialized.");
                    return;
                }

                ArrayList<String> keys = new ArrayList(tags.keySet());
                Collections.sort(keys);

                String output = "";
                for (String key : keys) {
                    if (output.length() > 0) {
                        output += ", ";
                    }

                    output += key + ":" + tags.get(key);
                }

                sender.sendChatToPlayer("Current tags:");
                sender.sendChatToPlayer(output);
            } else if (params[1].equals("clear")) {
                if (params.length == 3) {
                    String tag = params[2];

                    if (tag.equals("range")) {
                        sender.sendChatToPlayer("Can't remove range.");
                    } else {
                        marker.removeTag(tag);
                        sender.sendChatToPlayer("Removed tag " + tag + ".");
                    }
                    return;
                }

                sender.sendChatToPlayer("Usage: /w tag clear <tag>");
            } else {
                boolean okay = false;
                int value = -1;
                String tag = null;

                if (params.length == 2) {
                    tag = params[1];

                    Integer savedValue = marker.getTag(tag, false);

                    if (savedValue == null) {
                        sender.sendChatToPlayer("Tag " + tag + " is not set.");
                    } else {
                        sender.sendChatToPlayer("Tag " + tag + " has value " + savedValue + ".");
                    }

                    return;
                } else if (params.length == 3) {
                    tag = params[1];

                    if (params[2].equals("-")) {
                        if (tag.equals("range")) {
                            sender.sendChatToPlayer("Can't remove range.");
                        } else {
                            marker.removeTag(tag);
                            sender.sendChatToPlayer("Removed tag " + tag + ".");
                        }
                        return;
                    } else {
                        try {
                            value = Integer.parseInt(params[2]);
                            okay = true;
                        } catch (NumberFormatException e) { }
                    }
                }

                if (!okay) {
                    sender.sendChatToPlayer("Usage: /w tag <tag> [level]");
                    return;
                }

                marker.setTag(tag, value);
                Integer savedValue = marker.getTag(tag, false);
                if (savedValue == null || savedValue != value) {
                    sender.sendChatToPlayer("Failed to set tag.");
                } else {
                    sender.sendChatToPlayer("Set tag " + tag + " to " + value + ".");
                }
            }
        } else if (params[0].equals("range")) {
            if (params.length < 2) {
                Integer range = marker.getRange();

                if (range == null) {
                    sender.sendChatToPlayer("This house flag is uninitialized.");
                } else {
                    sender.sendChatToPlayer("This house flag has range " + range + ".");
                }
            } else {
                boolean okay = false;
                int new_range = -1;
                if (params.length == 2) {
                    try {
                        new_range = Integer.parseInt(params[1]);
                        okay = true;
                    } catch (NumberFormatException e) { }
                }

                if (!okay) {
                    sender.sendChatToPlayer("Usage: /w range [value]");
                    return;
                }

                marker.setRange(new_range);
                if (marker.getRange(false) != new_range) {
                    sender.sendChatToPlayer("Failed to set range.");
                } else {
                    sender.sendChatToPlayer("Range set to " + new_range + ".");
                }
            }
        } else {
            sender.sendChatToPlayer("Unknown subcommand.");
        }
    }

    /**
     * Adds the strings available in this command to the given list of tab completion options.
     */
    public List addTabCompletionOptions(ICommandSender sender, String[] params)
    {
        return Collections.EMPTY_LIST;
        //return params.length == 1 ? getListOfStringsMatchingLastWord(params, new String[] {"set", "add"}): (params.length == 2 && params[0].equals("set") ? getListOfStringsMatchingLastWord(params, new String[] {"day", "night"}): null);
    }
}
