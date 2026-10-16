package com.chatregexfilter;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MessageNode;
import net.runelite.api.FontTypeFace;
import net.runelite.api.Point;
import net.runelite.api.ScriptID;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Chat Regex Filter",
	description = "Hides or rewrites any chatbox line that matches your regular expressions, whatever chat type it came from",
	tags = {"chat", "filter", "regex", "hide", "spam", "clan", "broadcast", "mute", "replace"}
)
public class ChatRegexFilterPlugin extends Plugin
{
	// keep in sync with build.gradle
	public static final String VERSION = "1.1.0";

	private static final Logger log = LoggerFactory.getLogger(ChatRegexFilterPlugin.class);

	// the chatbox script fires this for every line it draws, with the message on the object stack
	private static final String CHAT_FILTER_CALLBACK = "chatFilterCheck";

	// the chatbox scroll area holds four dynamic children per line: sender, message, clan name, clan rank
	private static final int WIDGETS_PER_LINE = 4;
	private static final int SENDER_WIDGET_OFFSET = 0;
	private static final int MESSAGE_WIDGET_OFFSET = 1;

	private static final String COPY_RAW = "Copy raw text";
	private static final String COPY_FILTER = "Copy filter text";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatRegexFilterConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	private volatile LineFilter filter = new LineFilter("", true);
	private volatile Replacer replacer = new Replacer("", true);

	@Provides
	ChatRegexFilterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatRegexFilterConfig.class);
	}

	@Override
	protected void startUp()
	{
		rebuild(false);
		clientThread.invoke(client::refreshChat);
		log.info("Chat Regex Filter started");
	}

	@Override
	protected void shutDown()
	{
		filter = new LineFilter("", true);
		replacer = new Replacer("", true);
		clientThread.invoke(client::refreshChat);
		log.info("Chat Regex Filter stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!ChatRegexFilterConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		rebuild(true);
		// redraw so already visible lines pick up the new rules
		clientThread.invoke(client::refreshChat);
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!CHAT_FILTER_CALLBACK.equals(event.getEventName()))
		{
			return;
		}
		LineFilter current = filter;
		Replacer rewrite = replacer;
		if (current.isEmpty() && rewrite.isEmpty())
		{
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		Object[] objectStack = client.getObjectStack();
		int objectStackSize = client.getObjectStackSize();
		if (intStackSize < 3 || objectStackSize < 1)
		{
			return;
		}

		// stack layout: [.., show flag, message type, message id] and [.., message text]
		int messageId = intStack[intStackSize - 1];
		Object text = objectStack[objectStackSize - 1];
		String message = text instanceof String ? (String) text : null;

		String sender = null;
		if (config.includeSender())
		{
			MessageNode node = client.getMessages().get(messageId);
			if (node != null)
			{
				sender = node.getName();
			}
		}

		String candidate = LineFilter.normalize(sender, message, config.stripTags());
		if (!current.matches(candidate))
		{
			String replaced = rewrite.apply(message, config.stripTags());
			if (replaced == null)
			{
				return;
			}
			if (!Replacer.isBlank(replaced))
			{
				objectStack[objectStackSize - 1] = replaced;
				return;
			}
		}
		if (config.debugMode())
		{
			objectStack[objectStackSize - 1] = markLine(message, config.debugColor());
		}
		else
		{
			intStack[intStackSize - 3] = 0;
		}
	}

	// sender names are drawn from the message's own name, not the filter callback, so rewrite the built widgets.
	// Only the text changes; the widget's op name (what right-click Add friend/Report use) is left alone
	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() != ScriptID.BUILD_CHATBOX || !config.replaceNames())
		{
			return;
		}
		Replacer rewrite = replacer;
		if (rewrite.isEmpty())
		{
			return;
		}
		Widget scrollArea = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		if (scrollArea == null)
		{
			return;
		}
		Widget[] children = scrollArea.getDynamicChildren();
		if (children == null)
		{
			return;
		}
		for (int start = 0; start + WIDGETS_PER_LINE <= children.length; start += WIDGETS_PER_LINE)
		{
			Widget sender = children[start + SENDER_WIDGET_OFFSET];
			if (sender == null || sender.isHidden())
			{
				continue;
			}
			String label = sender.getText();
			if (label == null || label.isEmpty())
			{
				continue;
			}
			String replaced = rewrite.applyToName(label, config.stripTags());
			if (replaced == null)
			{
				continue;
			}
			FontTypeFace font = sender.getFont();
			int shift = font == null ? 0 : font.getTextWidth(label) - font.getTextWidth(replaced);
			sender.setText(replaced);
			if (shift == 0)
			{
				continue;
			}
			// the build script placed the rest of the line after the name at its old width, so pull it in
			sender.setOriginalWidth(Math.max(0, sender.getOriginalWidth() - shift));
			sender.revalidate();
			for (int i = start; i < start + WIDGETS_PER_LINE; i++)
			{
				Widget other = children[i];
				if (other != null && other != sender && other.getOriginalX() > sender.getOriginalX())
				{
					other.setOriginalX(other.getOriginalX() - shift);
					other.revalidate();
				}
			}
		}
	}

	// "X message" in one colour. Existing tags are removed so a </col> in the middle can't reset the colour
	static String markLine(String message, Color color)
	{
		String plain = Text.removeTags(message == null ? "" : message);
		return ColorUtil.wrapWithColorTag("X " + plain, color);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!config.copyMenu())
		{
			return;
		}
		Widget scrollArea = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		Point mouse = client.getMouseCanvasPosition();
		if (scrollArea == null || scrollArea.isHidden() || mouse == null
			|| !scrollArea.getBounds().contains(mouse.getX(), mouse.getY()))
		{
			return;
		}
		Widget[] children = scrollArea.getDynamicChildren();
		if (children == null)
		{
			return;
		}
		Widget line = null;
		for (int i = MESSAGE_WIDGET_OFFSET; i < children.length; i += WIDGETS_PER_LINE)
		{
			Widget child = children[i];
			if (child != null && !child.isHidden() && child.contains(mouse))
			{
				line = child;
				break;
			}
		}
		if (line == null)
		{
			return;
		}

		// the widget holds what was drawn (after filtering and other plugins). Find the message behind it for the raw value
		String drawn = line.getText();
		MessageNode node = findNode(drawn);
		String raw = node != null ? node.getValue() : drawn;
		String sender = config.includeSender() && node != null ? node.getName() : null;
		String filterText = LineFilter.normalize(sender, raw, config.stripTags());

		client.getMenu().createMenuEntry(1)
			.setOption(COPY_FILTER)
			.setTarget("")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> copyToClipboard(filterText));
		client.getMenu().createMenuEntry(1)
			.setOption(COPY_RAW)
			.setTarget("")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> copyToClipboard(raw));
	}

	// the drawn text may carry a timestamp or our debug X in front, so match a message whose plain text ends the line.
	// Longest match wins, newest on a tie
	private MessageNode findNode(String drawn)
	{
		String plainDrawn = Text.removeTags(drawn == null ? "" : drawn);
		MessageNode best = null;
		int bestLength = 0;
		for (MessageNode node : client.getMessages())
		{
			String value = node.getValue();
			if (value == null)
			{
				continue;
			}
			String plain = Text.removeTags(value);
			if (plain.isEmpty() || !plainDrawn.endsWith(plain))
			{
				continue;
			}
			if (plain.length() > bestLength || (plain.length() == bestLength && best != null && node.getId() > best.getId()))
			{
				best = node;
				bestLength = plain.length();
			}
		}
		return best;
	}

	private static void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	private void rebuild(boolean announce)
	{
		LineFilter next = new LineFilter(config.patterns(), config.caseInsensitive());
		Replacer nextReplacer = new Replacer(config.replacements(), config.caseInsensitive());
		filter = next;
		replacer = nextReplacer;
		List<String> errors = new ArrayList<>(next.getErrors());
		errors.addAll(nextReplacer.getErrors());
		for (String error : errors)
		{
			log.warn("Chat Regex Filter: invalid pattern, {}", error);
		}
		if (announce && config.reportInvalid() && !errors.isEmpty()
			&& client.getGameState() == GameState.LOGGED_IN)
		{
			String summary = "Chat Regex Filter: " + errors.size()
				+ (errors.size() == 1 ? " pattern is" : " patterns are")
				+ " not valid regex and will be ignored (" + errors.get(0) + ")";
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(summary)
				.build());
		}
	}
}
