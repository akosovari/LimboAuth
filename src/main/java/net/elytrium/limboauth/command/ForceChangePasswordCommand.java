/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboauth.command;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.handler.AuthSessionHandler;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.elytrium.limboauth.utils.SuggestUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ForceChangePasswordCommand implements SimpleCommand {

  private final ProxyServer server;
  private final Dao<RegisteredPlayer, String> playerDao;

  private final String message;
  private final String successful;
  private final String notSuccessful;
  private final Component usage;

  public ForceChangePasswordCommand(ProxyServer server, Dao<RegisteredPlayer, String> playerDao) {
    this.server = server;
    this.playerDao = playerDao;

    this.message = Settings.IMP.MAIN.STRINGS.FORCE_CHANGE_PASSWORD_MESSAGE;
    this.successful = Settings.IMP.MAIN.STRINGS.FORCE_CHANGE_PASSWORD_SUCCESSFUL;
    this.notSuccessful = Settings.IMP.MAIN.STRINGS.FORCE_CHANGE_PASSWORD_NOT_SUCCESSFUL;
    this.usage = LegacyComponentSerializer.legacyAmpersand().deserialize(Settings.IMP.MAIN.STRINGS.FORCE_CHANGE_PASSWORD_USAGE);
  }

  @Override
  public List<String> suggest(SimpleCommand.Invocation invocation) {
    return SuggestUtils.suggestPlayers(invocation.arguments(), this.server);
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 2) {
      String nickname = args[0];
      String newPassword = args[1];

      try {
        UpdateBuilder<RegisteredPlayer, String> updateBuilder = this.playerDao.updateBuilder();
        updateBuilder.where().eq("LOWERCASENICKNAME", nickname.toLowerCase(Locale.ROOT));
        updateBuilder.updateColumnValue("HASH", AuthSessionHandler.genHash(newPassword));
        updateBuilder.update();

        this.server.getPlayer(nickname).ifPresent(player ->
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(MessageFormat.format(this.message, newPassword)))
        );

        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(MessageFormat.format(this.successful, nickname)));
      } catch (SQLException e) {
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(MessageFormat.format(this.notSuccessful, nickname)));
        e.printStackTrace();
      }

      return;
    }

    source.sendMessage(this.usage);
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return invocation.source().hasPermission("limboauth.admin.forcechangepassword");
  }
}
