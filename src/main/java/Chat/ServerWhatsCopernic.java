package Chat;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.HashMap;

public class ServerWhatsCopernic {
    public void main(String[] args) {
        // Crear un mapa para almacenar los IDs de los clientes y sus sockets correspondientes
        HashMap<Integer, Socket> clients = new HashMap<>();
        int nextClientId = 1; // Contador para asignar IDs de cliente

        try {
            // Crear un socket de servidor en el puerto 12345
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("El servidor está en funcionamiento y esperando clientes...");

            while (true) {
                // Aceptar conexiones entrantes de clientes
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket);

                // Asignar un ID de cliente único al cliente
                int clientId = nextClientId++;
                clients.put(clientId, clientSocket);

                // Crear un nuevo hilo para manejar el cliente
                ClientHandler clientHandler = new ClientHandler(clientId, clientSocket, clients);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ClientHandler extends Thread {
        private int clientId;
        private Socket clientSocket;
        private HashMap<Integer, Socket> clients;

        public ClientHandler(int clientId, Socket clientSocket, HashMap<Integer, Socket> clients) {
            this.clientId = clientId;
            this.clientSocket = clientSocket;
            this.clients = clients;
        }

        @Override
        public void run() {
            try {
                // Mensaje de bienvenida
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("Bienvenido a WhatsCopernic!! ");

                // Recibir mensaje del cliente
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                }

                // Logout
                System.out.println("Usuario " + clientId + " desconectado.");
                clientSocket.close();
                clients.remove(clientId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}