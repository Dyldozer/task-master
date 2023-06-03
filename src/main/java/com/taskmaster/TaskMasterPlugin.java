package com.taskmaster;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.http.api.RuneLiteAPI.GSON;
import static net.runelite.http.api.RuneLiteAPI.JSON;

@Slf4j
@PluginDescriptor(
	name = "Task Master"
)
public class TaskMasterPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	public OkHttpClient okHttpClient;
	@Inject
	private ClientToolbar clientToolbar;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private TaskMasterPanel taskMasterPanel;

	private final String[] SKILLSLIST = new String[]{"Attack", "Defence", "Strength", "Hitpoints", "Ranged", "Prayer", "Magic", "Cooking", "Woodcutting", "Fletching", "Fishing", "Firemaking", "Crafting", "Smithing", "Mining", "Herblore", "Agility", "Thieving", "Slayer", "Farming", "Runecraft", "Hunter", "Construction"};
	private NavigationButton navButton;
	private String username;

	@Inject
	private TaskMasterConfig config;

	int skillXP = 0;

	private CurrentTask task;

	private static final Pattern KC_PATTERN = Pattern.compile(
			"Your (?<pre>completion count for |subdued |completed )?(?<boss>.+?) (?<post>(?:(?:kill|harvest|lap|completion) )?(?:count )?)is: <col=ff0000>(?<kc>\\d+)</col>" );


	@Override
	protected void startUp() throws Exception
	{
		taskMasterPanel = new TaskMasterPanel(this, config, client, okHttpClient);

		final BufferedImage icon;
		icon = ImageUtil.loadImageResource(TaskMasterPlugin.class, "tmicon.png");



		navButton = NavigationButton.builder().tooltip("Task Master").icon(icon).priority(5)
				.panel(taskMasterPanel).build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{

	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) throws IOException {
		if (gameStateChanged.getGameState() == GameState.LOGGING_IN)
		{
			username = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) throws IOException {
		if (client.getGameState() != GameState.LOGGED_IN) return;

		if (username == null && client.getLocalPlayer().getName() != null)
		{
			username = client.getLocalPlayer().getName();
			getTask();
		}

	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged) {
		Skill skill = statChanged.getSkill();
		int currentXp = statChanged.getXp();
		if (task == null) return;
		if (skill.getName().equalsIgnoreCase(task.getObjective()) && skillXP != 0)
		{
			task.setCurrent(task.getCurrent() + (statChanged.getXp() - skillXP));
			SwingUtilities.invokeLater(() -> taskMasterPanel.updateCurrent(task.getCurrent()));
			skillXP = statChanged.getXp();
		}
		else if (skill.getName().equalsIgnoreCase(task.getObjective()) && skillXP == 0)
		{
			skillXP = statChanged.getXp();
		}
	}

	@Subscribe
	public void onChatMessage (ChatMessage event) throws IOException {
		if (task == null) return;
		String message = event.getMessage();
		Matcher kcmatcher = KC_PATTERN.matcher(message);
		if (kcmatcher.find())
		{
			final String boss = kcmatcher.group( "boss" );
			final int kc = Integer.parseInt( kcmatcher.group( "kc" ) );
			if (boss.equalsIgnoreCase(task.getObjective()))
			{
				HttpUrl url = HttpUrl.parse( "http://localhost:25820/api/updatetaskmaster");
				RequestBody body = RequestBody
						.create(JSON, GSON.toJson(new TaskUpdate("BOSS", client.getLocalPlayer().getName(), task.getObjective(), kc)));
				Request request = new Request.Builder()
						.url(url)
						.post(body)
						.build();
				Response response = okHttpClient.newCall(request).execute();
				String current = response.body().string();
				System.out.println(current);
				SwingUtilities.invokeLater(() ->
				{

				});
				response.close();
			}
		}
	}

	@Provides
	TaskMasterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TaskMasterConfig.class);
	}

	public void getTask() throws IOException {
		System.out.println(client.getLocalPlayer().getName());
		HttpUrl url = HttpUrl.parse( "http://localhost:25820/api/updatetaskmaster");
		RequestBody body = RequestBody
				.create(JSON, GSON.toJson(new TaskUpdate("NEW", client.getLocalPlayer().getName(), null, -1)));
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		Response response = okHttpClient.newCall(request).execute();
		String responseString = response.body().string();
		response.close();
		System.out.println(responseString);
		Gson gson = new Gson();
		task = gson.fromJson(responseString, CurrentTask.class);
		if (task.getStart() == -1)
		{
			body = RequestBody
					.create(JSON, GSON.toJson(new TaskUpdate("START", client.getLocalPlayer().getName(), null, client.getSkillExperience(Skill.valueOf(task.getObjective().toUpperCase())))));
			request = new Request.Builder()
					.url(url)
					.post(body)
					.build();
			response = okHttpClient.newCall(request).execute();
			responseString = response.body().string();
			task = gson.fromJson(responseString, CurrentTask.class);
			response.close();
		}
		SwingUtilities.invokeLater(() ->
		{
			taskMasterPanel.processTask(task);
		});
	}

	private void sendNewXpUpdateTask(JsonObject jsonObject, int skillXP) throws IOException {
		HttpUrl url = HttpUrl.parse( "http://localhost:25820/api/updatetaskmaster");
		RequestBody body = RequestBody
				.create(JSON, GSON.toJson(new TaskUpdate("SKILL", client.getLocalPlayer().getName(), jsonObject.get("objective").getAsString(), skillXP)));
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		Response response = okHttpClient.newCall(request).execute();
		String current = response.body().string();
		System.out.println(current);
		task = new CurrentTask(
				jsonObject.get("task").getAsString(),
				jsonObject.get("objective").getAsString(),
				jsonObject.get("start").getAsInt() == -1 ? skillXP : jsonObject.get("start").getAsInt(),
				jsonObject.get("current").getAsInt(),
				jsonObject.get("goal").getAsInt());
		SwingUtilities.invokeLater(() ->
		{
			taskMasterPanel.processTask(task);
		});
	}
}
