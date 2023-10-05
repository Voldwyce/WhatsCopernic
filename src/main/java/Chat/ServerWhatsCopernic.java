package Chat;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Scanner;

public class ServerWhatsCopernic {
    public static Scanner sc = new Scanner(System.in);
    public static Connection cn;

    public static void main(String[] args) throws IOException {

        // Crear un mapa para almacenar los IDs de los clientes y sus sockets correspondientes
        HashMap<Integer, Socket> clients = new HashMap<>();
        int nextClientId = 1;

        ServerSocket serverSocket = new ServerSocket(42069);
        System.out.println("WhatsCopernic está esperando usuarios...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Usuario conectado: " + clientSocket);

            int clientId = nextClientId++;
            clients.put(clientId, clientSocket);

            ClientHandler clientHandler = new ClientHandler(clientId, clientSocket, clients);
            clientHandler.start();
        }
    }

    public static boolean iniciarSesion(String usuario, String pwd) {

        try {
            String query = "SELECT * FROM usuarios WHERE username = ? AND pswd = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, usuario);
            preparedStatement.setString(2, pwd);
            ResultSet result = preparedStatement.executeQuery();

            return result.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean crearCuenta(String usuario, String pwd) {

        try {
            String selectSql = "SELECT username FROM usuarios WHERE username = ?";
            PreparedStatement selectStatement = cn.prepareStatement(selectSql);
            selectStatement.setString(1, usuario);
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                return false;
            } else {
                String insertSql = "INSERT INTO usuarios (username, pswd) VALUES (?, ?)";
                PreparedStatement insertStatement = cn.prepareStatement(insertSql);
                insertStatement.setString(1, usuario);
                insertStatement.setString(2, pwd);
                int rowCount = insertStatement.executeUpdate();
                return rowCount > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class ClientHandler extends Thread {
        // Conexion servidor
        static {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                cn = DriverManager.getConnection("jdbc:mysql://localhost:3306/whatscopernic", "admin", "Test123!");
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
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

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    System.out.println("Cliente " + clientId + " dice: " + clientMessage);

                    // Dividir el mensaje en partes
                    String[] partes = clientMessage.split(" ");
                    if (partes.length != 3) { // Cambiado a 3 para incluir usuario, contraseña y comando
                        out.println("Comando inválido"); // Responder al cliente con un mensaje de error
                    } else {
                        String comando = partes[0];
                        String usuario = partes[1];
                        String pwd = partes[2];

                        switch (comando) {
                            case "login":
                                if (iniciarSesion(usuario, pwd)) {
                                    out.println("true");
                                } else {
                                    out.println("Credenciales incorrectas");
                                }
                                break;
                            case "create":
                                if (crearCuenta(usuario, pwd)) {
                                    out.println("true");
                                } else {
                                    out.println("Error al crear la cuenta");
                                }
                                break;
                            default:
                                out.println("Comando inválido");
                                break;
                        }
                    }
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
