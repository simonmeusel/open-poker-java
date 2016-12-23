package ui.text;

import java.io.IOException;

import main.connection.Client;
import main.connection.Player;
import main.connection.Update;
import main.core.Action;
import main.core.Card;

public class ClientCLI extends CLI {

	private Client client;
	
	public static void main(String[] args) {
		new ClientCLI();
	}

	public ClientCLI() {
		super();
		System.out.println("Host / IP:");
		String host = scanner.nextLine();
		if (host == "") host = "127.0.0.1";
		System.out.println("Port (10101):");
		int port = Integer.parseInt(scanner.nextLine());
		System.out.println("Username:");
		String username = scanner.nextLine();
		System.out.println("Room (0):");
		int room = Integer.parseInt(scanner.nextLine());
		try {
			client = new Client(host, port, username, room);
		} catch (Exception e) {
			System.out.println("Start up error!");
			e.printStackTrace();
			return;
		}

		startCLI();
	}

	@Override
	protected void onCommand(String command, String[] args) {
		System.out.println();
		switch (command) {
		case "action":
			System.out.println("Action");
			switch (args.length) {
			case 1:
				try {
					client.sendAction(Action.valueOf(args[0].toUpperCase()));
				} catch (IllegalArgumentException e) {
					System.out.println("Action not found!");
				} catch (IOException e) {
					System.out.println("Network error! Could not send Action.");
					//TODO restore connection?
				}
				break;
			case 2:
				try {
					client.sendAction(Action.valueOf(args[0].toUpperCase()), Integer.parseInt(args[1]));
				} catch (IllegalArgumentException e) {
					System.out.println("Action not found!");
				} catch (IOException e) {
					System.out.println("Network error! Could not send Action.");
					//TODO restore connection?
				}
				break;
			default:
				System.out.println("Wrong use!");
				break;
			}
			break;
		case "info":
			System.out.println("Information");
			Update update = client.getUpdate();
			System.out.println("Infos");
			System.out.println("  Small blind: " + update.getSmallBlind());
			System.out.println("IDs");
			System.out.println("  Current Player ID: " + update.getCurrentPlayer());
			System.out.println("  Button ID: " + update.getButtonId());
			System.out.println("  Big Blind ID: " + update.getBigBlindId());
			System.out.println("  Small Blind ID: " + update.getSmallBlindId());
			System.out.println("Table");
			System.out.println("  Current Pot: " + update.getCurrentPot());
			System.out.println("  Community Cards: ");
			for (Card card : update.getCommunityCards()) {
				System.out.println("    " + card.getColor()+ " " + card.getValue());
			}
			System.out.println("Players");
			System.out.println("  id: CurrentBet Money");
			for (int playerId : update.getPlayers().keySet()) {
				Player player = update.getPlayers().get(playerId);
				System.out.println("  " + playerId + ": " + player.getCurrentBet() + " " + player.getMoney());
			}
			System.out.println("You");
			System.out.println("  ID: " + update.getYourId());
			for (Card card : update.getYourCards()) {
				System.out.println("  " + card.getColor()+ " " + card.getValue());
			}
			break;
		case "exit":
		case "q":
			// terminate
			client.close();
			break;
		case "help":
			System.out.println("Commands:");
			System.out.println("info - Display game information");
			System.out.println("action <action> [amount] - Send an action to the server");
			System.out.println("  <action> may be one of: check, call, bet, raise, fold");
			System.out.println("exit, q - Quit game");
			break;
		default:
			// Help
			System.out.println("Command not found!");
			onCommand("help", null);
			break;
		}
	}

	@Override
	protected boolean checkRunningCondition() {
		return client.isRunning();
	}

}
