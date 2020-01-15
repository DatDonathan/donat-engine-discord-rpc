package at.jojokobi.donatengine.presence;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;

public class DiscordGamePresence implements GamePresencePlatform{

	private String clientId;
	private String steamId;
	private boolean running;
	
	private JoinListener join = null;
	private JoinRequestListener joinRequest = null;
	
	
	public DiscordGamePresence(String clientId, String steamId) {
		super();
		this.clientId = clientId;
		this.steamId = steamId;
	}

	@Override
	public void init() {
		System.out.println("Init");
		running = true;
		DiscordRPC rpc = DiscordRPC.INSTANCE;
		DiscordEventHandlers handlers = new DiscordEventHandlers();
		handlers.ready = user -> {System.out.println("RPC Ready");};
		handlers.joinGame = s -> {
			System.out.println("Joining " + s);
			if (join != null) {
				join.onJoin(s);
			}
		};
		handlers.joinRequest = d -> {
			System.out.println("Join Request from " + d.username + "#" + d.discriminator);
			if (joinRequest != null) {
				if (joinRequest.onJoinRequest(new GameJoinRequest(d.username + "#" + d.discriminator, "Discord"))) {
					rpc.Discord_Respond(d.userId, DiscordRPC.DISCORD_REPLY_YES);
				}
				else {
					rpc.Discord_Respond(d.userId, DiscordRPC.DISCORD_REPLY_NO);
				}
			}
		};
		rpc.Discord_Initialize(clientId, handlers, true, steamId);
		new Thread (() -> {
			while (running) {
				rpc.Discord_RunCallbacks();
				System.out.println("RPC_Update");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			rpc.Discord_Shutdown();
		}, "Discord-RPC-Thread").start();
	}

	@Override
	public void updatePresence(GamePresence presence) {
		DiscordRPC rpc = DiscordRPC.INSTANCE;
		if (presence == null) {
			rpc.Discord_ClearPresence();
		}
		else {
			DiscordRichPresence pre = convert(presence);
			rpc.Discord_UpdatePresence(pre);
		}
	}

	@Override
	public void end() {
		System.out.println("End presence");
		running = false;
	}
	
	private DiscordRichPresence convert (GamePresence presence) {
		DiscordRichPresence pre = new DiscordRichPresence();
		pre.state = presence.getState();
		pre.details = presence.getDetails();
		pre.startTimestamp = presence.getStartTimestamp();
		pre.endTimestamp = presence.getEndTimestamp();
		pre.largeImageKey = presence.getLargeImageKey();
		pre.largeImageText = presence.getLargeImageText();
		pre.smallImageKey = presence.getSmallImageKey();
		pre.smallImageText = presence.getSmallImageText();
		pre.partyId = presence.getPartyId();
		pre.partySize = presence.getPartySize();
		pre.partyMax = presence.getPartyMax();
		pre.matchSecret = presence.getMatchSecret();
		pre.joinSecret = presence.getJoinSecret();
		pre.spectateSecret = presence.getSpectateSecret();
		pre.instance = presence.isInstance() ? (byte) 1 : (byte) 0;
		
		return pre;
	}

	@Override
	public synchronized void setListeners(JoinListener join, JoinRequestListener joinRequest) {
		this.join = join;
		this.joinRequest = joinRequest;
	}
	
}
