package io.mrarm.irc.connection;

import java.util.List;

import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.setting.ReconnectIntervalSetting;

/**
 * Defines reconnect decision rules for a server connection.
 * <p>
 * This class contains policy only: <br>
 * - whether reconnect is enabled <br>
 * - how long to wait before each attempt
 * <p>
 * It does NOT execute reconnects or schedule timers.
 */
public class ReconnectPolicy {

    public int getReconnectDelay(int attemptNumber) {
        if (!AppSettings.isReconnectEnabled())
            return -1;

        List<ReconnectIntervalSetting.Rule> rules = AppSettings.getReconnectIntervalRules();

        if (rules.isEmpty())
            return -1;

        int att = 0;
        for (ReconnectIntervalSetting.Rule rule : rules) {
            att += rule.repeatCount;
            if (attemptNumber < att)
                return rule.reconnectDelay;
        }

        return rules.get(rules.size() - 1).reconnectDelay;
    }
}
