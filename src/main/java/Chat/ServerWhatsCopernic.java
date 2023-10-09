package Chat;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.HashMap;

public class ServerWhatsCopernic {
    public static Connection cn;

    public static void main(String[] args) throws IOException {
        // Crear un mapa para almacenar los IDs de los clientes y sus nombres de usuario correspondientes
        HashMap<Integer, String> clients = new HashMap<>();
        int nextClientId = 1;

        ServerSocket serverSocket = new ServerSocket(42069);
        System.out.println("WhatsCopernic está esperando usuarios...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Usuario conectado: " + clientSocket);

            int clientId = nextClientId++;
            clients.put(clientId, null);

            ClientHandler clientHandler = new ClientHandler(clientId, clientSocket, clients);
            clientHandler.start();
        }
    }

    public static class ClientHandler extends Thread {
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
        private HashMap<Integer, String> clients;

        public ClientHandler(int clientId, Socket clientSocket, HashMap<Integer, String> clients) {
            this.clientId = clientId;
            this.clientSocket = clientSocket;
            this.clients = clients;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientMessage;

                while ((clientMessage = in.readLine()) != null) {
                    System.out.println("Cliente " + clientId + " dice: " + clientMessage);

                    String[] parts = clientMessage.split(" ");
                    String command = parts[0];

                    switch (command) {
                        case "login":
                            if (parts.length < 3) {
                                out.println("Comando incorrecto");
                            } else {
                                String usuario = parts[1];
                                String pwd = parts[2];
                                if (iniciarSesion(usuario, pwd)) {
                                    clients.put(clientId, usuario);
                                    out.println("true");
                                } else {
                                    out.println("Credenciales incorrectas");
                                }
                            }
                            break;
                        case "create":
                            if (parts.length < 3) {
                                out.println("Comando incorrecto");
                            } else {
                                String usuario = parts[1];
                                String pwd = parts[2];
                                if (crearCuenta(usuario, pwd)) {
                                    out.println("true");
                                    out.println("Cuenta creada, inicie sesión");
                                    clients.put(clientId, usuario); // Registrar el usuario
                                } else {
                                    out.println("Error al crear la cuenta");
                                }
                            }
                            break;
                        case "listar":
                            String userList = listarUsuarios(clients);
                            out.println(userList);
                            break;
                        case "mensaje":
                            if (parts.length < 3) {
                                out.println("Comando incorrecto");
                            } else {
                                String destinoUsuario = parts[1];
                                String mensaje = clientMessage.substring(command.length() + destinoUsuario.length() + 2);
                                enviarMensaje(clientId, destinoUsuario, mensaje, clients);
                            }
                            break;
                        case "logout":
                            out.println("true");
                            clients.remove(clientId);
                            clientSocket.close();
                            return;
                        default:
                            out.println("Comando incorrecto");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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

        public static String listarUsuarios(HashMap<Integer, String> clients) {
            StringBuilder userList = new StringBuilder("Usuarios conectados: ");
            for (String username : clients.values()) {
                if (username != null) {
                    userList.append(username).append(", ");
                }
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return userList.toString();
        }

        // Implementa la lógica para enviar mensajes a otros usuarios aquí
        public static void enviarMensaje(int remitenteId, String destinoUsuario, String mensaje, HashMap<Integer, String> clients) {
            // Implementa esta función según tus necesidades específicas
        }
    }
}