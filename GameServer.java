import java.io.*;
import java.net.*;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class GameServer extends JFrame implements GameConstants {

	protected JTextArea msgBoard;
	protected JScrollPane scrollPane;
	protected DataOutputStream outstream1;
	protected DataOutputStream outstream2;

	public static void main(String[] args) {
		GameServer server = new GameServer();
	}

	public GameServer() {
		msgBoard = new JTextArea();

		// Create a scroll pane to hold text area
		scrollPane = new JScrollPane(msgBoard);

		// Add the scroll pane to the frame
		add(scrollPane, BorderLayout.CENTER);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(300, 300);
		setTitle("GameServer");
		setVisible(true);

		try {
			// Create a server socket
			ServerSocket serverSocket = new ServerSocket(8000);
			msgBoard.append(new Date() + ": Server started at socket 8000\n");

			// Number a session
			int sessionNo = 1;

			// Ready to create a session for every two players
			while (true) {
				msgBoard.append(new Date()
						+ ": Wait for players to join session " + sessionNo
						+ '\n');

				// Connect to player 1
				Socket player1 = serverSocket.accept();

				msgBoard.append(new Date() + ": Player 1 joined session "
						+ sessionNo + '\n');
				msgBoard.append("Player 1's IP address"
						+ player1.getInetAddress().getHostAddress() + '\n');

				outstream1 = new DataOutputStream(player1.getOutputStream());

				// Create a new thread for this session of player1
				new Thread(new ServerRunnable(this, player1, PLAYER1)).start();

				// Notify that the player is Player 1
				outstream1.writeDouble(PLAYER1);

				// Connect to player 2
				Socket player2 = serverSocket.accept();

				msgBoard.append(new Date() + ": Player 2 joined session "
						+ sessionNo + '\n');
				msgBoard.append("Player 2's IP address"
						+ player2.getInetAddress().getHostAddress() + '\n');

				outstream2 = new DataOutputStream(player2.getOutputStream());
				// Create a new thread for this session of player2
				new Thread(new ServerRunnable(this, player2, PLAYER2)).start();

				// Notify that the player is Player 2
				outstream2.writeDouble(PLAYER2);

				// If there's already 2 people
				outstream1.writeDouble(READY);
				outstream2.writeDouble(READY);

				// Display this session and increment session number
				msgBoard.append(new Date() + ": Start a thread for session "
						+ sessionNo++ + '\n');
			}
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}
}

class ServerRunnable implements Runnable, GameConstants {
	private Socket socket;
	private GameServer server;
	private int playerID;

	public ServerRunnable(GameServer server, Socket socket, int player) {
		this.server = server;
		this.socket = socket;
		playerID = player;
	}

	public void run() {
		DataInputStream instream;
		DataOutputStream outstream = null;

		try {
			instream = new DataInputStream(socket.getInputStream());

			while (true) {
				double message = instream.readDouble();
				server.msgBoard.append("Received from " + playerID + " CMD = "
						+ message + "\n");
				if (playerID == PLAYER1)
					outstream = server.outstream2;
				else if (playerID == PLAYER2)
					outstream = server.outstream1;
				try {
					if (outstream != null)
						outstream.writeDouble(message);
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			server.msgBoard.append("Remove connection: " + socket + "\n");
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}
}