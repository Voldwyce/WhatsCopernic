package Chat;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.HashMap;
import java.security.MessageDigest;
import java.util.Properties;

public class ServerWhatsCopernic {
    public static Connection cn;
    public static DataInputStream in;
    public static DataOutputStream out;
    public static ServerConfiguration serverConfig;


    public static void main(String[] args) throws IOException {
        loadServerConfiguration();

        // Crear un HashMap para guardar los clientes conectados
        HashMap<Integer, String> clients = new HashMap<>();
        int nextClientId = 1;
        int maxConnections = serverConfig.maximoConexiones; // Obtén el límite desde la configuración

        ServerSocket serverSocket = new ServerSocket(42069);
        System.out.println("WhatsCopernic está esperando usuarios...");

        // Deja entrar a gente mientras no se supere el maximo del fichero de configuracion
        while (true) {
            if (clients.size() < maxConnections) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Usuario conectado: " + clientSocket);

                int clientId = nextClientId++;
                clients.put(clientId, null);

                ClientHandler clientHandler = new ClientHandler(clientId, clientSocket, clients);
                clientHandler.start();
            } else {
                System.out.println("Se ha alcanzado el límite de conexiones. Espere a que alguien se desconecte.");
                try {
                    Thread.sleep(5000); // Espera 5 segundos antes de volver a comprobar
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class ClientHandler extends Thread {
        // Conexión a la base de datos
        static {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                cn = DriverManager.getConnection("jdbc:mysql://localhost:3306/whatscopernic",  serverConfig.userBdp, serverConfig.pwdBdp);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }

        public static String hashPassword(String password) {
            // Hashing de la contraseña
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = md.digest(password.getBytes());

                StringBuilder hexHash = new StringBuilder();
                for (byte b : hashBytes) {
                    hexHash.append(String.format("%02x", b));
                }

                return hexHash.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
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
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());
                String clientMessage;

                while (true) {
                    try {
                        clientMessage = in.readUTF();
                    } catch (EOFException e) {
                        break;
                    }

                    System.out.println("Cliente " + clientId + " dice: " + clientMessage);

                    String[] partes = clientMessage.split(" ");
                    String comando = partes[0];

                    switch (comando) {
                        case "login":
                            if (partes.length < 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String usuario = partes[1];
                                String pwd = partes[2];
                                String hashedPwd = hashPassword(pwd);

                                if (iniciarSesion(usuario, hashedPwd)) {
                                    out.writeUTF("true");
                                    clients.put(clientId, usuario);
                                } else {
                                    out.writeUTF("Credenciales incorrectas");
                                }
                            }
                            break;
                        case "create":
                            if (partes.length < 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String usuario = partes[1];
                                String pwd = partes[2];
                                String hashedPwd = hashPassword(pwd);
                                if (crearCuenta(usuario, hashedPwd)) {
                                    out.writeUTF("true");
                                    clients.put(clientId, usuario);
                                } else {
                                    out.writeUTF("Error al crear la cuenta");
                                }
                            }
                            break;
                        case "listar":
                            String userList = listarUsuarios(clients);
                            out.writeUTF(userList);
                            break;
                        case "logout":
                            out.writeUTF("true");
                            clients.remove(clientId);
                            clientSocket.close();
                            return;
                        default:
                            out.writeUTF("Comando incorrecto");
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

            return userList.toString();
        }

    }
    static class ServerConfiguration {
        public int tamanoMaximoArchivo;
        public int maximoConexiones;
        public String pwdBdp;
        public String userBdp;
        public String nombreServidor;
        public String rutaAlmacenamientoArchivos;

    }


    private static void loadServerConfiguration() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Carga las variables del archivo de configuración
        serverConfig = new ServerConfiguration();
        serverConfig.tamanoMaximoArchivo = Integer.parseInt(properties.getProperty("tamanoMaximoArchivo"));
        serverConfig.maximoConexiones = Integer.parseInt(properties.getProperty("maximoConexiones"));
        serverConfig.pwdBdp = properties.getProperty("pwdBdp");
        serverConfig.userBdp = properties.getProperty("userBdp");
        serverConfig.nombreServidor = properties.getProperty("nombreServidor");
        serverConfig.rutaAlmacenamientoArchivos = properties.getProperty("rutaAlmacenamientoArchivos");

    }
}
