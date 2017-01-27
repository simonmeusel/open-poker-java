package main.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import main.connection.Packet;
import main.connection.Table;
import main.connection.Update;
import main.core.Action;

public class PlayerController implements Runnable {

	private Socket socket;
	private Server server;
	private TableController tableController;
	private Thread thread;
	private boolean running = true;
	private boolean loggedIn = false;

	private int id;
	private String username;

	private ObjectInputStream in;
	private ObjectOutputStream out;

	public PlayerController(Socket socket, Server server)
			throws IOException, ClassNotFoundException, IllegalStateException, IllegalArgumentException {
		this.socket = socket;
		this.server = server;

		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());

		System.out.println("New player connected");

		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		while (running) {
			try {
				read();
			} catch (ClassNotFoundException e) {
				System.out.println("Recieved corrupt client paket.");
				try {
					in.reset();
				} catch (IOException e1) {
					System.out.println("Fatal: Could not reset input stream.");
					e1.printStackTrace();
					running = false;
				}
			} catch (IOException e) {
				if (running) {
					System.out.println("Fatal: Network error!");
					running = false;
				}
			}
		}
		System.out.println("Kick player " + id);
		server.getAuthenticationController().logOut(username);
		tableController.removePlayer(id);
		// TODO send info to client
		if (!socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Read all arrived packets
	 */
	private void read() throws ClassNotFoundException, IOException {
		Packet packet = (Packet) in.readObject();
		parsePacket(packet);
	}

	/**
	 * Parse a packet
	 * 
	 * @param packet
	 *            Packet to parse
	 */
	private void parsePacket(Packet packet) {
		HashMap<String, Object> data = packet.getData();
		switch (packet.getType()) {
		case "login":
			if (loggedIn)
				break;
			if (server.getAuthenticationController().validate((String) data.get("username"),
					(String) data.get("password"))) {
				username = (String) data.get("username");
				loggedIn = true;
				sendPacket(new Packet("accept", null));
				System.out.println("Player logged in with username " + username);
			} else {
				sendPacket(new Packet("decline", null));
				close();
			}
			break;
		case "signup":
			if (loggedIn)
				break;
			try {
				server.getAuthenticationController().registerUser((String) data.get("username"),
						(String) data.get("password"));
				username = (String) data.get("username");
				loggedIn = true;
				sendPacket(new Packet("accept", null));
				System.out.println("Player signed up with username " + username);
			} catch (Exception e) {
				e.printStackTrace();
				sendPacket(new Packet("decline", null));
				close();
			}
			break;
		case "getTables":
			if (loggedIn) {
				HashMap<String, Object> infoData = new HashMap<>();
				ArrayList<Table> tables = new ArrayList<>();
				for (int tableControllerId : server.getTables().keySet()) {
					TableController tableController = server.getTableController(id);
					tables.add(new Table(tableControllerId, tableController.getMoney(),
							tableController.getPlayerAmount(), tableController.getMaxPlayerAmount()));
				}
				infoData.put("tables", tables);
				Packet infoPacket = new Packet("tables", infoData);
				sendPacket(infoPacket);
			} else {
				sendPacket(new Packet("decline", null));
			}
			break;
		case "join":
			if (loggedIn && tableController == null) {
				int tableId = (int) data.get("room");
				tableController = server.getTableController(tableId);

				try {
					tableController.addPlayerController(this);
					sendPacket(new Packet("accept", null));
				} catch (IllegalStateException e) {
					sendPacket(new Packet("decline", null));
				}
			} else {
				sendPacket(new Packet("decline", null));
			}
			break;
		case "action":
			Action action = (Action) data.get("action");
			int amount = (Integer) data.get("amount");
			tableController.action(id, action, amount);
			break;
		default:
			break;
		}
	}

	private void sendPacket(Packet packet) {
		try {
			out.reset();
			out.writeObject(packet);
			out.flush();
		} catch (IOException e) {
			System.out.println("Failed to send package " + packet.getType() + " to client " + id);
			try {
				out.reset();
				out.writeObject(packet);
				out.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
				// Kick player
				running = false;
			}
		}
	}

	public void resend(main.core.Table table) {
		System.out.println("Resend data to player " + id);
		HashMap<String, Object> data = new HashMap<>();
		data.put("update", new Update(table, id));
		Packet packet = new Packet("update", data);
		sendPacket(packet);
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void close() {
		running = false;
		try {
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
