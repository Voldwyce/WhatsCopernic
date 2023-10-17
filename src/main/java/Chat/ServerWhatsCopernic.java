package Chat;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.HashMap;
import java.security.MessageDigest;
import java.util.Properties;

public class ServerWhatsCopernic {
    public static Connection cn;

    private static ServerConfiguration serverConfig;

    public static void main(String[] args) {
        loadServerConfiguration();

        // Crear un HashMap para guardar los clientes conectados
        HashMap<Integer, String> clients = new HashMap<>();
        int nextClientId = 1;
        int maxConnections = serverConfig.maximoConexiones; // Obtén el límite desde la configuración

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(42069);
        } catch (IOException e) {
            System.out.println("Error al crear el socket del servidor");
            throw new RuntimeException(e);
        }
        System.out.println("WhatsCopernic está esperando usuarios...");

        // Deja entrar a gente mientras no se supere el máximo del fichero de configuración
        while (true) {
            if (clients.size() < maxConnections) {
                Socket clientSocket;
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    System.out.println("Error al aceptar la conexión del cliente");
                    throw new RuntimeException(e);
                }
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
                cn = DriverManager.getConnection("jdbc:mysql://localhost:3306/whatscopernic", serverConfig.userBdp, serverConfig.pwdBdp);
            } catch (ClassNotFoundException | SQLException e) {
                System.out.println("Error al conectar con la base de datos");
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
                System.out.println("Error al hashear la contraseña");
                e.printStackTrace();
                return null;
            }
        }

        private final int clientId;
        private final Socket clientSocket;
        private final HashMap<Integer, String> clients;

        public ClientHandler(int clientId, Socket clientSocket, HashMap<Integer, String> clients) {
            this.clientId = clientId;
            this.clientSocket = clientSocket;
            this.clients = clients;
        }

        @Override
        public void run() {
            try {
                DataInputStream in;
                DataOutputStream out;
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());
                String clientMessage;

                while (true) {
                    try {
                        clientMessage = in.readUTF();
                    } catch (EOFException e) {
                        System.out.println("Usuario desconectado: " + clientSocket);
                        clients.remove(clientId);
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
                                    out.writeUTF("Error al crear la cuenta, usuario en uso");
                                }
                            }
                            break;
                        case "listar":
                            String userList = listarUsuarios(clients);
                            out.writeUTF(userList);
                            break;
                        case "mensaje":
                            if (partes.length < 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String destinoUsuario = partes[1];
                                String mensaje = clientMessage.substring(comando.length() + destinoUsuario.length() + 2);
                                if (enviarMensaje(clientId, destinoUsuario, mensaje, clients)) {
                                    out.writeUTF("Mensaje enviado correctamente a " + destinoUsuario);
                                } else {
                                    out.writeUTF("Error al enviar el mensaje");
                                }
                            }
                            break;
                        case "mensajeGrupo":
                            if (partes.length < 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String destinoGrupo = partes[1];
                                String mensaje = clientMessage.substring(comando.length() + destinoGrupo.length() + 2);
                                if (enviarMensajeGrupo(clientId, destinoGrupo, mensaje, clients)) {
                                    out.writeUTF("Mensaje enviado correctamente a " + destinoGrupo);
                                } else {
                                    out.writeUTF("Error al enviar el mensaje");
                                }
                            }
                            break;
                        case "listarmensajes":
                            if (partes.length < 2) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String nombreUsuario = partes[1];
                                String mensajes = listarMensajesUsuario(clientId, nombreUsuario, clients);
                                out.writeUTF(mensajes);
                            }
                            break;

                        case "listarmensajesgrupo":
                            if (partes.length < 2) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String nombreGrupo = partes[1];
                                String mensajes = listarMensajesGrupo(clientId, nombreGrupo, clients);
                                out.writeUTF(mensajes);
                            }
                            break;
                        case "creargrupo":
                            if (partes.length < 2) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String grupo = partes[1];
                                int idGrupo = crearGrupo(clientId, grupo, cn, clients);
                                if (idGrupo > 0) {
                                    System.out.println("Grupo creado con éxito");
                                    out.writeUTF("true"); // Grupo creado con éxito
                                } else {
                                    out.writeUTF("Error al crear el grupo");
                                }
                            }
                            break;
                        case "eliminargrupo":
                            if (partes.length < 2) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String grupo = partes[1];
                                boolean grupoEliminado = eliminarGrupo(clientId, grupo, cn, clients);
                                if (grupoEliminado) {
                                    System.out.println("Grupo eliminado con éxito");
                                    out.writeUTF("true"); // Grupo eliminado con éxito
                                } else {
                                    System.out.println("Error al eliminar el grupo");
                                    out.writeUTF("Error al eliminar el grupo"); // Error al eliminar el grupo
                                }
                            }
                            break;
                        case "veradmin":
                            if (partes.length > 4) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String grupo = partes[1];
                                boolean esAdmin = tienePermisosDeAdmin(clientId, clients, obtenerIdGrupoDesdeDB(grupo, cn));
                                if (esAdmin) {
                                    System.out.println("El usuario es administrador del grupo");
                                    out.writeUTF("true"); // El usuario es administrador del grupo
                                } else {
                                    System.out.println("El usuario no es administrador del grupo");
                                    out.writeUTF("false"); // El usuario no es administrador del grupo
                                }
                            }
                            break;
                        case "agregarusuario":
                            if (partes.length > 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String nombreUsuario = partes[1];
                                String grupo = partes[2];
                                boolean anadido = anadirMiembroAGrupo(nombreUsuario, grupo);
                                if (anadido) {
                                    System.out.println("Miembro añadido al grupo con éxito");
                                    out.writeUTF("true"); // Miembro añadido al grupo con éxito
                                } else {
                                    System.out.println("Error al añadir al miembro al grupo");
                                    out.writeUTF("Error al añadir al miembro al grupo"); // Error al añadir al miembro al grupo
                                }
                            }
                            break;
                        case "eliminarusuario":
                            if (partes.length > 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String nombreUsuario = partes[1];
                                String grupo = partes[2];
                                boolean eliminado = eliminarMiembroDeGrupo(nombreUsuario, grupo);
                                if (eliminado) {
                                    System.out.println("Usuario eliminado del grupo con éxito");
                                    out.writeUTF("true"); // Miembro eliminado del grupo con éxito
                                } else {
                                    System.out.println("Error al eliminar al usuario del grupo");
                                    out.writeUTF("Error al eliminar al usuario del grupo"); // Error al eliminar al miembro del grupo
                                }
                            }
                            break;
                        case "listargrupos":
                            if (partes.length > 2) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String grupos = listarGrupos();
                                out.writeUTF(grupos);
                            }
                            break;
                        case "vermiembros":
                            if (partes.length > 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String grupo = partes[1];
                                String miembros = listarMiembrosDeGrupo(grupo);
                                out.writeUTF(miembros);
                            }
                            break;
                        case "darpermisos":
                            if (partes.length > 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String nombreUsuario = partes[1];
                                String grupo = partes[2];
                                boolean permisos = darPermisos(nombreUsuario, grupo);
                                if (permisos) {
                                    System.out.println("Permisos otorgados con éxito");
                                    out.writeUTF("true"); // Permisos otorgados con éxito
                                } else {
                                    System.out.println("Error al otorgar permisos");
                                    out.writeUTF("Error al otorgar permisos"); // Error al otorgar permisos
                                }
                            }
                            break;
                        case "quitarpermisos":
                            if (partes.length > 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String nombreUsuario = partes[1];
                                String grupo = partes[2];
                                boolean permisos = quitarPermisos(nombreUsuario, grupo);
                                if (permisos) {
                                    System.out.println("Permisos revocados con éxito");
                                    out.writeUTF("true"); // Permisos revocados con éxito
                                } else {
                                    System.out.println("Error al revocar permisos");
                                    out.writeUTF("Error al revocar permisos"); // Error al revocar permisos
                                }
                            }
                            break;
                        case "enviararchivo":
                            if (partes.length > 3) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String nombreDestinatario = partes[1];
                                String archivo = partes[2];
                                boolean enviado = enviarArchivo(clientId, nombreDestinatario, archivo, clients);
                                if (enviado) {
                                    System.out.println("Archivo enviado con éxito");
                                    out.writeUTF("true"); // Archivo enviado con éxito
                                } else {
                                    System.out.println("Error al enviar el archivo");
                                    out.writeUTF("Error al enviar el archivo"); // Error al enviar el archivo
                                }
                            }
                            break;
                        case "listararchivos":
                            if (partes.length > 2) {
                                out.writeUTF("Comando incorrecto");
                            } else {
                                String archivos = listarArchivos(clientId, clients);
                                out.writeUTF(archivos);
                            }
                            break;

                        case "logout":
                            out.writeUTF("true");
                            logout(clientId, clients);
                            clientSocket.close();
                            return;
                        default:
                            out.writeUTF("Comando incorrecto");
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Usuario desconectado: " + clientSocket);
                clients.remove(clientId);
            }
        }

        public synchronized static boolean iniciarSesion(String usuario, String pwd) {
            try {
                String query = "SELECT * FROM usuarios WHERE username = ? AND pswd = ?";
                PreparedStatement preparedStatement = cn.prepareStatement(query);
                preparedStatement.setString(1, usuario);
                preparedStatement.setString(2, pwd);
                ResultSet result = preparedStatement.executeQuery();

                return result.next();
            } catch (SQLException e) {
                System.out.println("Error al iniciar sesión");
                e.printStackTrace();
                return false;
            }
        }

        public synchronized static boolean crearCuenta(String usuario, String pwd) {
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
                    insertStatement.setString(1, usuario.toLowerCase());
                    insertStatement.setString(2, pwd);
                    int rowCount = insertStatement.executeUpdate();
                    return rowCount > 0;
                }
            } catch (SQLException e) {
                System.out.println("Error al crear la cuenta");
                e.printStackTrace();
                return false;
            }
        }

        public static boolean enviarMensaje(int remitenteId, String destinoUsuario, String mensaje, HashMap<Integer, String> clients) {
            try {
                int idRemitente = obtenerIdUsuarioDesdeDB(clients.get(remitenteId), cn);
                int idDestinatario = obtenerIdUsuarioDesdeDB(destinoUsuario, cn);

                String insertSql = "INSERT INTO mensajes (id_usuario_in, mensaje, id_usuario_out) VALUES (?, ?, ?)";
                PreparedStatement insertStatement = cn.prepareStatement(insertSql);
                insertStatement.setInt(1, idRemitente);
                insertStatement.setString(2, mensaje);
                insertStatement.setInt(3, idDestinatario);

                int rowCount = insertStatement.executeUpdate();
                return rowCount > 0;
            } catch (SQLException e) {
                System.out.println("Error al enviar el mensaje");
                e.printStackTrace();
                return false;
            }
        }

        public static boolean enviarMensajeGrupo(int remitenteId, String destinoGrupo, String mensaje, HashMap<Integer, String> clients) {
            try {
                int idRemitente = obtenerIdUsuarioDesdeDB(clients.get(remitenteId), cn);
                String verificacionQuery = "SELECT id_grupo FROM grupos WHERE grp_nombre = ?";
                PreparedStatement verificacionStatement = cn.prepareStatement(verificacionQuery);
                verificacionStatement.setString(1, destinoGrupo);
                ResultSet verificacionResult = verificacionStatement.executeQuery();

                if (verificacionResult.next()) {
                    int idGrupoDestinatario = verificacionResult.getInt("id_grupo");

                    String pertenenciaQuery = "SELECT COUNT(*) FROM grp_usuarios WHERE id_usuario = ? AND id_grupo = ?";
                    PreparedStatement pertenenciaStatement = cn.prepareStatement(pertenenciaQuery);
                    pertenenciaStatement.setInt(1, idRemitente);
                    pertenenciaStatement.setInt(2, idGrupoDestinatario);
                    ResultSet pertenenciaResult = pertenenciaStatement.executeQuery();

                    if (pertenenciaResult.next() && pertenenciaResult.getInt(1) == 1) {
                        // El remitente pertenece al grupo, ahora puedes insertar el mensaje
                        String insertSql = "INSERT INTO mensajes (id_usuario_in, mensaje, id_grupo) VALUES (?, ?, ?)";
                        PreparedStatement insertStatement = cn.prepareStatement(insertSql);
                        insertStatement.setInt(1, idRemitente);
                        insertStatement.setString(2, mensaje);
                        insertStatement.setInt(3, idGrupoDestinatario);

                        int rowCount = insertStatement.executeUpdate();
                        return rowCount > 0;
                    } else {
                        // El remitente no pertenece al grupo
                        return false;
                    }
                } else {
                    // El grupo destino no existe
                    return false;
                }
            } catch (SQLException e) {
                System.out.println("Error al enviar el mensaje");
                e.printStackTrace();
                return false;
            }
        }
    }

    public static String listarMensajesUsuario(int remitenteId, String nombreUsuario, HashMap<Integer, String> clients) {
        try {
            int idRemitente = obtenerIdUsuarioDesdeDB(clients.get(remitenteId), cn);
            int idDestinatario = obtenerIdUsuarioDesdeDB(nombreUsuario, cn);

            if (idDestinatario == -1) {
                return "Usuario no encontrado en la base de datos";
            }

            String query = "SELECT mensaje FROM mensajes WHERE (id_usuario_in = ? AND id_usuario_out = ?)";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setInt(1, idDestinatario);
            preparedStatement.setInt(2, idRemitente);

            ResultSet resultSet = preparedStatement.executeQuery();
            StringBuilder mensajes = new StringBuilder();

            while (resultSet.next()) {
                String mensaje = resultSet.getString("mensaje");
                mensajes.append(mensaje).append("\n");
            }

            return mensajes.toString();
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al recibir los mensajes del usuario";
        }
    }

    public static String listarMensajesGrupo(int remitenteId, String nombreGrupo, HashMap<Integer, String> clients) {
        try {
            int idRemitente = obtenerIdUsuarioDesdeDB(clients.get(remitenteId), cn);
            String verificacionQuery = "SELECT id_grupo FROM grupos WHERE grp_nombre = ?";
            PreparedStatement verificacionStatement = cn.prepareStatement(verificacionQuery);
            verificacionStatement.setString(1, nombreGrupo);
            ResultSet verificacionResult = verificacionStatement.executeQuery();

            if (verificacionResult.next()) {
                int idGrupoDestinatario = verificacionResult.getInt("id_grupo");

                String pertenenciaQuery = "SELECT COUNT(*) FROM grp_usuarios WHERE id_usuario = ? AND id_grupo = ?";
                PreparedStatement pertenenciaStatement = cn.prepareStatement(pertenenciaQuery);
                pertenenciaStatement.setInt(1, idRemitente);
                pertenenciaStatement.setInt(2, idGrupoDestinatario);
                ResultSet pertenenciaResult = pertenenciaStatement.executeQuery();

                if (pertenenciaResult.next() && pertenenciaResult.getInt(1) == 1) {
                    // muestra usuario y mensaje enviado
                    String query = "SELECT username, mensaje FROM mensajes INNER JOIN usuarios ON mensajes.id_usuario_in = usuarios.id_usuario WHERE id_grupo = ?";
                    PreparedStatement preparedStatement = cn.prepareStatement(query);
                    preparedStatement.setInt(1, idGrupoDestinatario);

                    ResultSet resultSet = preparedStatement.executeQuery();
                    StringBuilder mensajes = new StringBuilder();

                    while (resultSet.next()) {
                        String mensaje = resultSet.getString("mensaje");
                        String usuario = resultSet.getString("username");
                        mensajes.append(usuario).append(": ").append(mensaje).append("\n");
                    }

                    return mensajes.toString();
                } else {
                    return "El grupo no existe";
                }
            } else {
                return "El grupo no existe";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al recibir los mensajes del grupo";
        }
    }

    private static int crearGrupo(int clientId, String grupo, Connection cn, HashMap<Integer, String> clients) {
        try {
            // Asegúrate de que el cliente tenga un ID de usuario válido
            int idUsuario = obtenerIdUsuarioDesdeDB(clients.get(clientId), cn);
            if (idUsuario != -1) {
                // Ahora puedes continuar con la inserción del grupo
                String insertGrupoQuery = "INSERT INTO grupos (grp_nombre, id_usuario) VALUES (?, ?)";
                PreparedStatement insertGrupoStatement = cn.prepareStatement(insertGrupoQuery, Statement.RETURN_GENERATED_KEYS);
                insertGrupoStatement.setString(1, grupo);
                insertGrupoStatement.setInt(2, idUsuario);
                int rowCount = insertGrupoStatement.executeUpdate();

                if (rowCount > 0) {
                    ResultSet generatedKeys = insertGrupoStatement.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int idGrupo = generatedKeys.getInt(1);

                        // Ahora, inserta la relación en la tabla grp_usuarios
                        String insertRelacionQuery = "INSERT INTO grp_usuarios (id_grupo, id_usuario, grp_permisos) VALUES (?, ?, ?)";
                        PreparedStatement insertRelacionStatement = cn.prepareStatement(insertRelacionQuery);
                        insertRelacionStatement.setInt(1, idGrupo);
                        insertRelacionStatement.setInt(2, idUsuario);
                        insertRelacionStatement.setInt(3, 1); // Puedes ajustar los permisos aquí

                        int relacionRowCount = insertRelacionStatement.executeUpdate();

                        if (relacionRowCount > 0) {
                            return idGrupo; // Grupo creado con éxito
                        }
                    }
                }
            }
            return 0; // Error al crear el grupo
        } catch (SQLException e) {
            e.printStackTrace();
            return 0; // Error SQL
        }
    }

    private static String listarGrupos() {
        try {
            String query = "SELECT grp_nombre FROM grupos";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            StringBuilder grupos = new StringBuilder("Grupos:\n");

            while (resultSet.next()) {
                String grupo = resultSet.getString("grp_nombre");
                grupos.append(grupo).append("\n");
            }
            return grupos.toString();
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al listar grupos.";
        }
    }

    private static int obtenerIdUsuarioDesdeDB(String username, Connection cn) {
        try {
            String query = "SELECT id_usuario FROM usuarios WHERE username = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("id_usuario");
            } else {
                System.out.println("No se encontró el usuario en la base de datos.");
                return -1; // Devuelve -1 para indicar que no se encontró el usuario en la base de datos.
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1; // Devuelve -1 en caso de error SQL.
        }
    }

    public static boolean eliminarGrupo(int clientId, String grupo, Connection cn, HashMap<Integer, String> clients) {
        String username = clients.get(clientId);

        if (username == null) {
            System.out.println("No se pudo obtener el nombre de usuario del cliente.");
            return false;
        }

        // Obtener el ID de usuario del cliente actual
        int idUsuario = obtenerIdUsuarioDesdeDB(username, cn);

        if (idUsuario == -1) {
            System.out.println("No se pudo obtener el ID de usuario de la base de datos.");
            return false;
        }

        String query = "SELECT id_grupo, id_usuario FROM grupos WHERE grp_nombre = ?";
        try {
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, grupo);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int idGrupo = resultSet.getInt("id_grupo");
                int idCreadorGrupo = resultSet.getInt("id_usuario");

                // Verifica si el cliente es el creador del grupo y tiene permisos de administrador
                if (idCreadorGrupo == idUsuario && tienePermisosDeAdmin(clientId, clients, idGrupo)) {
                    // Antes de eliminar el grupo, primero elimina los registros relacionados en grp_usuarios
                    if (eliminarUsuariosDelGrupo(idGrupo)) {
                        // Ahora elimina el grupo
                        String deleteQuery = "DELETE FROM grupos WHERE id_grupo = ?";
                        PreparedStatement deleteStatement = cn.prepareStatement(deleteQuery);
                        deleteStatement.setInt(1, idGrupo);
                        int rowCount = deleteStatement.executeUpdate();

                        if (rowCount > 0) {
                            return true; // Grupo eliminado con éxito
                        } else {
                            return false; // Error al eliminar el grupo
                        }
                    }
                } else {
                    System.out.println("El cliente no es el creador del grupo o no tiene permisos de administrador.");
                    return false; // El cliente no es el creador del grupo o no tiene permisos de administrador
                }
            } else {
                System.out.println("No existe el grupo");
                return false; // No existe el grupo
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar el grupo");
            e.printStackTrace();
        }
        return false;
    }

    private static boolean eliminarUsuariosDelGrupo(int idGrupo) { // Todos los usuarios del grupo
        try {
            String deleteUsuariosQuery = "DELETE FROM grp_usuarios WHERE id_grupo = ?";
            PreparedStatement deleteUsuariosStatement = cn.prepareStatement(deleteUsuariosQuery);
            deleteUsuariosStatement.setInt(1, idGrupo);
            int rowCount = deleteUsuariosStatement.executeUpdate();

            if (rowCount > 0) {
                return true; // Usuario/s eliminado/s del grupo con éxito
            } else {
                return false; // Error al eliminar usuario/s del grupo
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar usuario/s del grupo");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean anadirMiembroAGrupo(String nombreUsuario, String grupo) {
        try {
            String query = "SELECT id_usuario FROM usuarios WHERE username = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, nombreUsuario);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int idUsuario = resultSet.getInt("id_usuario");
                int idGrupo = obtenerIdGrupoDesdeDB(grupo, cn);
                String insertQuery = "INSERT INTO grp_usuarios (id_grupo, id_usuario, grp_permisos) VALUES (?, ?, ?)";
                PreparedStatement insertStatement = cn.prepareStatement(insertQuery);
                insertStatement.setInt(1, idGrupo); // Ajusta el ID del grupo aquí
                insertStatement.setInt(2, idUsuario);
                insertStatement.setInt(3, 0); // Ajusta los permisos aquí

                int rowCount = insertStatement.executeUpdate();
                return rowCount > 0;
            } else {
                System.out.println("No existe el usuario");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error al añadir al miembro al grupo");
            e.printStackTrace();
            return false;
        }
    }

    private static boolean darPermisos(String nombreUuario, String grupo) {
        try {
            String query = "SELECT id_usuario FROM usuarios WHERE username = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, nombreUuario);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int idUsuario = resultSet.getInt("id_usuario");
                int idGrupo = obtenerIdGrupoDesdeDB(grupo, cn);
                String updateQuery = "UPDATE grp_usuarios SET grp_permisos = 1 WHERE id_grupo = ? AND id_usuario = ?";
                PreparedStatement updateStatement = cn.prepareStatement(updateQuery);
                updateStatement.setInt(1, idGrupo);
                updateStatement.setInt(2, idUsuario);

                int rowCount = updateStatement.executeUpdate();
                return rowCount > 0;
            } else {
                System.out.println("No existe el usuario");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error al otorgar permisos");
            e.printStackTrace();
            return false;
        }
    }

    private static boolean quitarPermisos(String nombreUsuario, String grupo) {
        try {
            String query = "SELECT id_usuario FROM usuarios WHERE username = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, nombreUsuario);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int idUsuario = resultSet.getInt("id_usuario");
                int idGrupo = obtenerIdGrupoDesdeDB(grupo, cn);
                String updateQuery = "UPDATE grp_usuarios SET grp_permisos = 0 WHERE id_grupo = ? AND id_usuario = ?";
                PreparedStatement updateStatement = cn.prepareStatement(updateQuery);
                updateStatement.setInt(1, idGrupo);
                updateStatement.setInt(2, idUsuario);

                int rowCount = updateStatement.executeUpdate();
                return rowCount > 0;
            } else {
                System.out.println("No existe el usuario");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error al revocar permisos");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean eliminarMiembroDeGrupo(String nombreUsuario, String grupo) { // Solo un usuario
        try {
            String query = "SELECT id_usuario FROM usuarios WHERE username = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, nombreUsuario);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int idUsuario = resultSet.getInt("id_usuario");
                int idGrupo = obtenerIdGrupoDesdeDB(grupo, cn);
                String deleteQuery = "DELETE FROM grp_usuarios WHERE id_grupo = ? AND id_usuario = ?";
                PreparedStatement deleteStatement = cn.prepareStatement(deleteQuery);
                deleteStatement.setInt(1, idGrupo);
                deleteStatement.setInt(2, idUsuario);

                int rowCount = deleteStatement.executeUpdate();
                return rowCount > 0;
            } else {
                System.out.println("No existe el usuario");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar al miembro del grupo");
            e.printStackTrace();
            return false;
        }
    }

    public static String listarMiembrosDeGrupo(String grupo) {
        try {
            String query = "SELECT username FROM usuarios INNER JOIN grp_usuarios ON usuarios.id_usuario = grp_usuarios.id_usuario INNER JOIN grupos ON grupos.id_grupo = grp_usuarios.id_grupo WHERE grupos.grp_nombre = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, grupo);
            ResultSet resultSet = preparedStatement.executeQuery();

            StringBuilder miembros = new StringBuilder("Miembros del grupo " + grupo + ":\n");

            while (resultSet.next()) {
                String miembro = resultSet.getString("username");
                miembros.append(miembro).append("\n");
            }
            return miembros.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al listar miembros del grupo.";
        }
    }

    public static int obtenerIdGrupoDesdeDB(String grupo, Connection cn) {
        try {
            String query = "SELECT id_grupo FROM grupos WHERE grp_nombre = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, grupo);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("id_grupo");
            } else {
                System.out.println("El grupo no existe en la base de datos.");
                return -1; // Devuelve -1 para indicar que el grupo no existe en la base de datos.
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1; // Devuelve -1 en caso de error SQL.
        }
    }

    private static boolean tienePermisosDeAdmin(int clientID, HashMap<Integer, String> clients, int idGrupo) {
        int idUsuario = obtenerIdUsuarioDesdeDB(clients.get(clientID), cn);
        try {
            String query = "SELECT grp_permisos FROM grp_usuarios WHERE id_usuario = ? AND id_grupo = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setInt(1, idUsuario);
            preparedStatement.setInt(2, idGrupo);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int permisos = resultSet.getInt("grp_permisos");
                return permisos == 1; // Comprobar si los permisos son de administrador (1)
            }
        } catch (SQLException e) {
            System.out.println("Error al comprobar los permisos");
            e.printStackTrace();
        }
        return false; // Si hay un error, no tiene permisos
    }

    public static boolean enviarArchivo(int clientID, String destinoUsuario, String rutaArchivoCompleta, HashMap<Integer, String> clients) {
        int idUsuario = obtenerIdUsuarioDesdeDB(clients.get(clientID), cn);
        try {
            String query = "SELECT id_usuario FROM usuarios WHERE username = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setString(1, destinoUsuario);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int idDestinatario = resultSet.getInt("id_usuario");

                // Divide la ruta completa para obtener el nombre del archivo
                String[] rutaPartes = rutaArchivoCompleta.split("\\\\");
                String nombreArchivo = rutaPartes[rutaPartes.length - 1];

                // Nombre archivo = current mili time
                String nombreArchivoServer = System.currentTimeMillis() + nombreArchivo;
                String rutaServidor = serverConfig.rutaAlmacenamientoArchivos + nombreArchivoServer;

                // Copiar el archivo a la ruta del servidor, si la carpeta no existe la creamos
                File carpetaAlmacenamiento = new File(serverConfig.rutaAlmacenamientoArchivos);
                if (!carpetaAlmacenamiento.exists()) {
                    carpetaAlmacenamiento.mkdir();
                }
                Path origenPath = Paths.get(rutaArchivoCompleta);
                Path destinoPath = Paths.get(rutaServidor);
                Files.copy(origenPath, destinoPath, StandardCopyOption.REPLACE_EXISTING);

                String insertSql = "INSERT INTO archivos (id_usuario_in, ruta_archivo, nombre_archivo, id_usuario_out) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStatement = cn.prepareStatement(insertSql);
                insertStatement.setInt(1, idUsuario);
                insertStatement.setString(2, rutaServidor);
                insertStatement.setString(3, nombreArchivo);
                insertStatement.setInt(4, idDestinatario);

                int rowCount = insertStatement.executeUpdate();
                return rowCount > 0;
            } else {
                return false;
            }
        } catch (SQLException | IOException e) {
            System.out.println("Error al enviar el archivo");
            e.printStackTrace();
            return false;
        }
    }
    public static String listarArchivos(int clientID, HashMap<Integer, String> clients) {
        try {
            int idUsuario = obtenerIdUsuarioDesdeDB(clients.get(clientID), cn);
            String query = "SELECT nombre_archivo FROM archivos WHERE id_usuario_out = ?";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            preparedStatement.setInt(1, idUsuario);
            ResultSet resultSet = preparedStatement.executeQuery();

            StringBuilder archivos = new StringBuilder("Archivos:\n");

            while (resultSet.next()) {
                String archivo = resultSet.getString("nombre_archivo");
                archivos.append(archivo).append("\n");
            }
            return archivos.toString();
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error al listar archivos.";
        }
    }

    public synchronized static String listarUsuarios(HashMap<Integer, String> clients) {

        StringBuilder userList = new StringBuilder("Usuarios Conectados: \n");
        for (String username : clients.values()) {
            if (username != null) {
                userList.append(username).append(", ");
            }
        }
        if (userList.length() > 0) {
            userList.deleteCharAt(userList.length() - 2);
        }

        userList.append("\nUsuarios Desconectados: \n");

        try {
            String query = "SELECT username FROM usuarios";
            PreparedStatement preparedStatement = cn.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String username = resultSet.getString("username");
                if (!clients.containsValue(username)) {
                    userList.append(username).append(", ");
                }
                if (resultSet.isLast()) {
                    userList.deleteCharAt(userList.length() - 2);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al listar usuarios");
            e.printStackTrace();
        }

        return userList.toString();
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
            System.out.println("Error al cargar el archivo de configuración");
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

    public synchronized static void logout(int clientId, HashMap<Integer, String> clients) {
        try {
            String username = clients.get(clientId);
            if (username != null) {
                clients.put(clientId, null);  // Marca al usuario como desconectado
                System.out.println("Cliente " + clientId + " se ha desconectado (" + username + ")");
            }


        } catch (Exception e) {
            System.out.println("Error al desconectar el cliente " + clientId);
            e.printStackTrace();
        }

    }
}