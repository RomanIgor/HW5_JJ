package org.example;
import lombok.Getter;

/**
 * 0. Разобраться с написанным кодом в классах Server и Client.
 * 1. Если в начале сообщения есть '@4' - то значит отсылаем сообщеине клиенту с идентификатором 4.
 * 2. Если в начале сообщения нет '@' - значит, это сообщение нужно послать остальным клиентам.
 * 3.* Добавить админское подключение, которое может кикать других клиентов.
 * 3.1 При подключении оно посылает спец. сообщение, подтверждающее, что это - админ.
 * 3.2 Теперь, если админ посылает сообщение kick 4 - то отключаем клиента с идентификатором 4.
 * 4.** Подумать, как лучше структурировать программу (раскидать код по классам).
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class Server {


    public static final int PORT = 8080;

    private static long clientIdCounter = 1L;
    private static Map<Long, SocketWrapper> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("The server is running on port " + PORT);
            while (true) {
                final Socket client = server.accept();
                final long clientId = clientIdCounter++;

                SocketWrapper wrapper = new SocketWrapper(clientId, client);
                System.out.println("A new client has been connected[" + wrapper + "]");
                clients.put(clientId, wrapper);
                new Thread(() -> {
                    try (Scanner input = wrapper.getInput(); PrintWriter output = wrapper.getOutput()) {
                        output.println("Connection successful. List of all clients: " + clients);

                        while (input.hasNextLine()) {
                            String clientInput = input.nextLine();
                            if (Objects.equals("q", clientInput)) {
                                clients.remove(clientId);
                                clients.values().forEach(it -> {
                                    it.getOutput().println("Client[" + clientId + "] disconnected");
                                });
                                System.out.println("Client[" + clientId + "] disconnected");
                                break;
                            }

                            try {
                                // Validate and parse the input
                                if (clientInput.startsWith("@")) {
                                    // Case 1: Message begins with '@'
                                    long destinationId = Long.parseLong(clientInput.substring(1, 2));
                                    SocketWrapper destination = clients.get(destinationId);
                                    if (destination != null) {
                                        destination.getOutput().println(clientInput.substring(3)); // Send message excluding '@X'
                                    } else {
                                        output.println("Invalid destination ID: " + destinationId);
                                    }
                                } else {
                                    // Case 2: Message does not begin with '@'
                                    clients.values().stream()
                                            .filter(it -> it.getId() != clientId)
                                            .forEach(it -> it.getOutput().println("Client[" + clientId + "]: " + clientInput));
                                }
                            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                // Handle parsing or format issues
                                output.println("Invalid input format: " + clientInput);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();


            }
        }
    }

}
